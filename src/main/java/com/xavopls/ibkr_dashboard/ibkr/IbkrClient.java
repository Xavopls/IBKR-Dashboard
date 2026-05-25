package com.xavopls.ibkr_dashboard.ibkr;

import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.client.DefaultEWrapper;
import com.ib.client.EClientSocket;
import com.ib.client.EJavaSignal;
import com.ib.client.EReader;
import com.ib.client.Execution;
import com.ib.client.ExecutionFilter;
import com.xavopls.ibkr_dashboard.config.IbkrProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class IbkrClient {

    private static final Logger log = LoggerFactory.getLogger(IbkrClient.class);

    private static final int TRADES_REQUEST_ID = 1;
    private static final int FLEX_REQUEST_ATTEMPTS = 4;
    private static final long FLEX_REQUEST_BACKOFF_MILLIS = 5_000L;
    private static final DateTimeFormatter IBKR_EXECUTION_TIME = DateTimeFormatter.ofPattern("yyyyMMdd  HH:mm:ss");
    private static final DateTimeFormatter FLEX_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter FLEX_SLASH_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final IbkrProperties properties;
    private final RestClient flexRestClient;

    public IbkrClient(IbkrProperties properties) {
        this.properties = properties;
        this.flexRestClient = RestClient.create();
    }

    public List<IbkrPositionSnapshot> getPositions(String accountId) {
        log.info("Requesting TWS positions account={}", accountId);
        EJavaSignal signal = new EJavaSignal();
        PositionSnapshotHandler handler = new PositionSnapshotHandler(accountId);
        EClientSocket client = new EClientSocket(handler, signal);
        List<IbkrPositionSnapshot> positions = requestPositions(client, signal, handler, properties.getTws());
        log.info("Received TWS positions account={} count={}", accountId, positions.size());
        return positions;
    }

    public List<IbkrTradeExecution> getTrades(String accountId, LocalDate from, LocalDate to) {
        return getHistoricalTrades(accountId, from, to);
    }

    public IbkrAccountProfile getAccountProfile(String accountId, LocalDate from, LocalDate to) {
        IbkrProperties.Flex flex = properties.getFlex();
        requireText(flex.getToken(), "IBKR_FLEX_TOKEN is required for account profile sync.");
        requireText(flex.getTradesQueryId(), "IBKR_FLEX_TRADES_QUERY_ID is required for account profile sync.");

        log.info("Requesting IBKR Flex account profile account={} from={} to={}", accountId, from, to);
        String referenceCode = requestFlexStatement(flex, from, to);
        String statement = fetchFlexStatement(flex, referenceCode);
        IbkrAccountProfile profile = parseAccountProfile(statement, accountId);
        log.info("Received IBKR Flex account profile account={} currency={} dateOpened={} dateFunded={}",
                accountId, profile.currency(), profile.dateOpened(), profile.dateFunded());
        return profile;
    }

    public List<IbkrTradeExecution> getHistoricalTrades(String accountId, LocalDate from, LocalDate to) {
        IbkrProperties.Flex flex = properties.getFlex();
        requireText(flex.getToken(), "IBKR_FLEX_TOKEN is required for historical trade sync.");
        requireText(flex.getTradesQueryId(), "IBKR_FLEX_TRADES_QUERY_ID is required for historical trade sync.");

        log.info("Requesting IBKR Flex trades account={} from={} to={}", accountId, from, to);
        String referenceCode = requestFlexStatement(flex, from, to);
        String statement = fetchFlexStatement(flex, referenceCode);
        List<IbkrTradeExecution> trades = parseFlexTrades(statement, accountId);
        log.info("Received IBKR Flex trades account={} count={}", accountId, trades.size());
        return trades;
    }

    public List<IbkrTradeExecution> getSessionTrades(String accountId) {
        log.info("Requesting TWS session executions account={}", accountId);
        EJavaSignal signal = new EJavaSignal();
        TradeExecutionHandler handler = new TradeExecutionHandler(accountId, TRADES_REQUEST_ID);
        EClientSocket client = new EClientSocket(handler, signal);
        List<IbkrTradeExecution> trades = requestTrades(client, signal, handler, properties.getTws());
        log.info("Received TWS session executions account={} count={}", accountId, trades.size());
        return trades;
    }

    private String requestFlexStatement(IbkrProperties.Flex flex, LocalDate from, LocalDate to) {
        for (int attempt = 1; attempt <= FLEX_REQUEST_ATTEMPTS; attempt++) {
            log.info("Sending IBKR Flex statement request queryId={} from={} to={} attempt={}/{}",
                    flex.getTradesQueryId(), from, to, attempt, FLEX_REQUEST_ATTEMPTS);
            String response = flexRestClient.get()
                    .uri(sendRequestUri(flex, from, to))
                    .retrieve()
                    .body(String.class);

            Document document = parseXml(response);
            String status = text(document, "Status");
            if ("Success".equalsIgnoreCase(status)) {
                String referenceCode = requireText(text(document, "ReferenceCode"), "IBKR Flex response did not include a reference code.");
                log.info("IBKR Flex statement request accepted referenceCode={}", referenceCode);
                return referenceCode;
            }

            String errorMessage = firstText(text(document, "ErrorMessage"), status);
            if (isRetryableFlexError(errorMessage) && attempt < FLEX_REQUEST_ATTEMPTS) {
                waitBeforeFlexRetry(attempt, errorMessage);
                continue;
            }

            throw new IllegalStateException("IBKR Flex request failed: " + errorMessage);
        }

        throw new IllegalStateException("IBKR Flex request failed after retries.");
    }

    private String fetchFlexStatement(IbkrProperties.Flex flex, String referenceCode) {
        log.info("Fetching IBKR Flex statement referenceCode={}", referenceCode);
        String response = flexRestClient.get()
                .uri(UriComponentsBuilder.fromUriString(flex.getBaseUrl() + "/GetStatement")
                        .queryParam("t", flex.getToken())
                        .queryParam("q", referenceCode)
                        .queryParam("v", flex.getVersion())
                        .toUriString())
                .retrieve()
                .body(String.class);

        Document document = parseXml(response);
        String status = text(document, "Status");
        if ("Fail".equalsIgnoreCase(status)) {
            throw new IllegalStateException("IBKR Flex statement fetch failed: " + firstText(text(document, "ErrorMessage"), status));
        }

        log.info("Fetched IBKR Flex statement referenceCode={}", referenceCode);
        return response;
    }

    private String sendRequestUri(IbkrProperties.Flex flex, LocalDate from, LocalDate to) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(flex.getBaseUrl() + "/SendRequest")
                .queryParam("t", flex.getToken())
                .queryParam("q", flex.getTradesQueryId())
                .queryParam("v", flex.getVersion());

        if (from != null && to != null) {
            builder.queryParam("fd", FLEX_DATE.format(from));
            builder.queryParam("td", FLEX_DATE.format(to));
        }

        return builder.toUriString();
    }

    private List<IbkrTradeExecution> parseFlexTrades(String xml, String accountId) {
        Document document = parseXml(xml);
        NodeList tradeNodes = document.getElementsByTagName("Trade");
        List<IbkrTradeExecution> trades = new ArrayList<>();

        if (tradeNodes.getLength() == 0) {
            log.warn("IBKR Flex response contained no Trade rows for account={}", accountId);
            return List.of();
        }

        for (int index = 0; index < tradeNodes.getLength(); index++) {
            Element trade = (Element) tradeNodes.item(index);
            String tradeAccountId = attr(trade, "accountId", "acctId", "account");
            if (tradeAccountId != null && !tradeAccountId.isBlank() && !Objects.equals(accountId, tradeAccountId)) {
                continue;
            }
            if (!isExecutionTrade(trade)) {
                continue;
            }

            trades.add(new IbkrTradeExecution(
                    firstText(tradeAccountId, accountId),
                    attr(trade, "ibExecID", "execID", "executionID", "executionId"),
                    attr(trade, "symbol", "underlyingSymbol"),
                    attr(trade, "description", "contractDescription", "symbol"),
                    attr(trade, "assetCategory", "assetClass", "securityType"),
                    attr(trade, "listingExchange", "exchange", "primaryExchange"),
                    attr(trade, "currency", "tradeCurrency"),
                    optionalDecimal(attr(trade, "fxRateToBase")),
                    parseFlexDate(attr(trade, "tradeDate", "dateTime")),
                    parseFlexDateTime(attr(trade, "dateTime")),
                    parseFlexDate(attr(trade, "settleDateTarget", "settlementDate")),
                    attr(trade, "buySell", "side"),
                    decimal(attr(trade, "quantity", "shares")),
                    decimal(attr(trade, "tradePrice", "price")),
                    optionalDecimal(attr(trade, "ibCommission", "commission")),
                    optionalDecimal(attr(trade, "netCash", "netAmount")),
                    optionalDecimal(attr(trade, "fifoPnlRealized", "realizedPnl"))
            ));
        }

        if (trades.isEmpty()) {
            log.warn("IBKR Flex response contained {} Trade rows, but none were accepted for account={}",
                    tradeNodes.getLength(), accountId);
            return List.of();
        }

        log.info("Parsed IBKR Flex trades account={} rawTradeRows={} acceptedExecutions={}",
                accountId, tradeNodes.getLength(), trades.size());
        return trades;
    }

    private IbkrAccountProfile parseAccountProfile(String xml, String accountId) {
        Document document = parseXml(xml);
        NodeList accountNodes = document.getElementsByTagName("AccountInformation");
        for (int index = 0; index < accountNodes.getLength(); index++) {
            Element account = (Element) accountNodes.item(index);
            String profileAccountId = attr(account, "accountId", "acctId", "account");
            if (profileAccountId != null && !profileAccountId.isBlank() && !Objects.equals(accountId, profileAccountId)) {
                continue;
            }

            return new IbkrAccountProfile(
                    firstText(profileAccountId, accountId),
                    attr(account, "currency", "baseCurrency"),
                    parseOptionalFlexDate(attr(account, "dateOpened")),
                    parseOptionalFlexDate(attr(account, "dateFunded"))
            );
        }

        return new IbkrAccountProfile(accountId, null, null, null);
    }

    private boolean isExecutionTrade(Element trade) {
        String levelOfDetail = attr(trade, "levelOfDetail");
        if (levelOfDetail != null && !"EXECUTION".equalsIgnoreCase(levelOfDetail)) {
            return false;
        }

        String executionId = attr(trade, "ibExecID", "execID", "executionID", "executionId");
        if (executionId == null || executionId.isBlank()) {
            return false;
        }

        return isPositiveDecimal(attr(trade, "quantity", "shares"))
                && isPositiveDecimal(attr(trade, "tradePrice", "price"));
    }

    private boolean isPositiveDecimal(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return new BigDecimal(value.replace(",", ""))
                .abs()
                .setScale(6, RoundingMode.HALF_UP)
                .compareTo(BigDecimal.ZERO) > 0;
    }

    private List<IbkrPositionSnapshot> requestPositions(EClientSocket client,
                                                        EJavaSignal signal,
                                                        PositionSnapshotHandler handler,
                                                        IbkrProperties.Tws tws) {
        Thread readerThread = null;
        Duration timeout = Duration.ofSeconds(tws.getTimeoutSeconds());

        try {
            client.eConnect(tws.getHost(), tws.getPort(), tws.getClientId());
            if (!client.isConnected()) {
                throw new IllegalStateException("Unable to connect to TWS at " + tws.getHost() + ":" + tws.getPort());
            }

            EReader reader = new EReader(client, signal);
            reader.start();

            readerThread = new Thread(
                    () -> processMessages(client, signal, reader, handler),
                    "ibkr-tws-reader-" + tws.getClientId()
            );
            readerThread.setDaemon(true);
            readerThread.start();

            handler.awaitReady(timeout);
            client.reqAccountUpdates(true, handler.accountId());

            return handler.awaitPositions(timeout);
        } finally {
            unsubscribe(client, handler.accountId());
            if (readerThread != null) {
                readerThread.interrupt();
            }
        }
    }

    private void processMessages(EClientSocket client,
                                 EJavaSignal signal,
                                 EReader reader,
                                 TwsCallbackHandler handler) {
        while (client.isConnected() && !Thread.currentThread().isInterrupted()) {
            signal.waitForSignal();
            try {
                reader.processMsgs();
            } catch (Exception ex) {
                handler.fail(ex);
                return;
            }
        }
    }

    private interface TwsCallbackHandler {

        void fail(Exception ex);
    }

    private void unsubscribe(EClientSocket client, String accountId) {
        if (!client.isConnected()) {
            return;
        }

        try {
            client.reqAccountUpdates(false, accountId);
        } catch (RuntimeException ignored) {
            // Keep the original sync/connect failure visible to the caller.
        }

        try {
            client.eDisconnect();
        } catch (RuntimeException ignored) {
            // Nothing else to do during cleanup.
        }
    }

    private List<IbkrTradeExecution> requestTrades(EClientSocket client,
                                                   EJavaSignal signal,
                                                   TradeExecutionHandler handler,
                                                   IbkrProperties.Tws tws) {
        Thread readerThread = null;
        Duration timeout = Duration.ofSeconds(tws.getTimeoutSeconds());

        try {
            client.eConnect(tws.getHost(), tws.getPort(), tws.getClientId());
            if (!client.isConnected()) {
                throw new IllegalStateException("Unable to connect to TWS at " + tws.getHost() + ":" + tws.getPort());
            }

            EReader reader = new EReader(client, signal);
            reader.start();

            readerThread = new Thread(
                    () -> processMessages(client, signal, reader, handler),
                    "ibkr-tws-reader-trades-" + tws.getClientId()
            );
            readerThread.setDaemon(true);
            readerThread.start();

            handler.awaitReady(timeout);
            client.reqExecutions(handler.requestId(), executionFilter(handler.accountId()));

            return handler.awaitTrades(timeout);
        } finally {
            disconnect(client);
            if (readerThread != null) {
                readerThread.interrupt();
            }
        }
    }

    private ExecutionFilter executionFilter(String accountId) {
        ExecutionFilter filter = new ExecutionFilter();
        filter.acctCode(accountId);
        return filter;
    }

    private void disconnect(EClientSocket client) {
        if (!client.isConnected()) {
            return;
        }

        try {
            client.eDisconnect();
        } catch (RuntimeException ignored) {
            // Nothing else to do during cleanup.
        }
    }

    private Document parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to parse IBKR Flex XML response", ex);
        }
    }

    private String text(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }

    private String attr(Element element, String... names) {
        for (String name : names) {
            if (element.hasAttribute(name)) {
                String value = element.getAttribute(name);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.replace(",", ""));
    }

    private BigDecimal optionalDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value.replace(",", ""));
    }

    private LocalDate parseFlexDate(String value) {
        if (value == null || value.isBlank()) {
            return LocalDate.now();
        }

        String date = value.trim();
        int separator = Math.min(nonNegativeOrMax(date.indexOf(';')), nonNegativeOrMax(date.indexOf(' ')));
        if (separator != Integer.MAX_VALUE) {
            date = date.substring(0, separator);
        }

        try {
            return LocalDate.parse(date, FLEX_DATE);
        } catch (RuntimeException ignored) {
            try {
                return LocalDate.parse(date);
            } catch (RuntimeException ignoredAgain) {
                try {
                    return LocalDate.parse(date, FLEX_SLASH_DATE);
                } catch (RuntimeException ignoredThird) {
                    return LocalDate.now();
                }
            }
        }
    }

    private LocalDate parseOptionalFlexDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseFlexDate(value);
    }

    private LocalDateTime parseFlexDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().replace(';', ' ');
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("dd/MM/yyyy HHmmss"));
        } catch (RuntimeException ignored) {
            try {
                return LocalDateTime.parse(normalized);
            } catch (RuntimeException ignoredAgain) {
                return parseFlexDate(value).atStartOfDay();
            }
        }
    }

    private int nonNegativeOrMax(int value) {
        return value >= 0 ? value : Integer.MAX_VALUE;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private boolean isRetryableFlexError(String message) {
        if (message == null) {
            return false;
        }

        String normalized = message.toLowerCase();
        return normalized.contains("too many requests")
                || normalized.contains("statement is not available")
                || normalized.contains("statement could not be generated");
    }

    private void waitBeforeFlexRetry(int attempt, String errorMessage) {
        long delayMillis = FLEX_REQUEST_BACKOFF_MILLIS * attempt;
        log.warn("IBKR Flex request failed with retryable response: {}. Retrying in {} seconds",
                errorMessage, delayMillis / 1000L);
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry IBKR Flex request", ex);
        }
    }

    private static class PositionSnapshotHandler extends DefaultEWrapper implements TwsCallbackHandler {

        private final String accountId;
        private final List<IbkrPositionSnapshot> positions = new ArrayList<>();
        private final CompletableFuture<Void> ready = new CompletableFuture<>();
        private final CompletableFuture<List<IbkrPositionSnapshot>> completed = new CompletableFuture<>();
        private final AtomicBoolean finished = new AtomicBoolean(false);

        private PositionSnapshotHandler(String accountId) {
            this.accountId = accountId;
        }

        private String accountId() {
            return accountId;
        }

        private void awaitReady(Duration timeout) {
            try {
                ready.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception ex) {
                throw new IllegalStateException("Timed out waiting for TWS API readiness", ex);
            }
        }

        private List<IbkrPositionSnapshot> awaitPositions(Duration timeout) {
            try {
                return completed.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception ex) {
                throw new IllegalStateException("Timed out waiting for TWS positions", ex);
            }
        }

        @Override
        public void fail(Exception ex) {
            ready.completeExceptionally(ex);
            completed.completeExceptionally(ex);
        }

        @Override
        public void nextValidId(int orderId) {
            ready.complete(null);
        }

        @Override
        public void updatePortfolio(Contract contract,
                                    Decimal position,
                                    double marketPrice,
                                    double marketValue,
                                    double averageCost,
                                    double unrealizedPNL,
                                    double realizedPNL,
                                    String accountName) {
            if (!Objects.equals(accountId, accountName)) {
                return;
            }

            positions.add(new IbkrPositionSnapshot(
                    accountName,
                    firstText(contract.symbol(), contract.localSymbol()),
                    firstText(contract.description(), contract.localSymbol(), contract.symbol()),
                    contract.getSecType(),
                    firstText(contract.primaryExch(), contract.exchange()),
                    contract.currency(),
                    toBigDecimal(position),
                    BigDecimal.valueOf(averageCost),
                    BigDecimal.valueOf(marketPrice),
                    BigDecimal.valueOf(unrealizedPNL)
            ));
        }

        @Override
        public void accountDownloadEnd(String accountName) {
            if (Objects.equals(accountId, accountName) && finished.compareAndSet(false, true)) {
                completed.complete(List.copyOf(positions));
            }
        }

        @Override
        public void error(Exception ex) {
            fail(ex);
        }

        @Override
        public void error(String message) {
            fail(new IllegalStateException(message));
        }

        @Override
        public void error(int id, long errorTime, int errorCode, String errorMsg, String advancedOrderRejectJson) {
            if (isFatal(errorCode)) {
                fail(new IllegalStateException("TWS API error " + errorCode + ": " + errorMsg));
            }
        }

        @Override
        public void connectionClosed() {
            if (!finished.get()) {
                fail(new IllegalStateException("TWS connection closed before positions finished"));
            }
        }

        private BigDecimal toBigDecimal(Decimal value) {
            return value == null || !value.isValid() ? BigDecimal.ZERO : BigDecimal.valueOf(value.value().doubleValue());
        }

        private boolean isFatal(int errorCode) {
            return errorCode == 502
                    || errorCode == 504
                    || errorCode == 1100
                    || errorCode == 1300
                    || (errorCode >= 300 && errorCode < 400);
        }

        private String firstText(String... values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            return null;
        }
    }

    private static class TradeExecutionHandler extends DefaultEWrapper implements TwsCallbackHandler {

        private final String accountId;
        private final int requestId;
        private final List<IbkrTradeExecution> trades = new ArrayList<>();
        private final CompletableFuture<Void> ready = new CompletableFuture<>();
        private final CompletableFuture<List<IbkrTradeExecution>> completed = new CompletableFuture<>();
        private final AtomicBoolean finished = new AtomicBoolean(false);

        private TradeExecutionHandler(String accountId, int requestId) {
            this.accountId = accountId;
            this.requestId = requestId;
        }

        private String accountId() {
            return accountId;
        }

        private int requestId() {
            return requestId;
        }

        private void awaitReady(Duration timeout) {
            try {
                ready.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception ex) {
                throw new IllegalStateException("Timed out waiting for TWS API readiness", ex);
            }
        }

        private List<IbkrTradeExecution> awaitTrades(Duration timeout) {
            try {
                return completed.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception ex) {
                throw new IllegalStateException("Timed out waiting for TWS trade executions", ex);
            }
        }

        @Override
        public void fail(Exception ex) {
            ready.completeExceptionally(ex);
            completed.completeExceptionally(ex);
        }

        @Override
        public void nextValidId(int orderId) {
            ready.complete(null);
        }

        @Override
        public void execDetails(int reqId, Contract contract, Execution execution) {
            if (reqId != requestId || !Objects.equals(accountId, execution.acctNumber())) {
                return;
            }

            trades.add(new IbkrTradeExecution(
                    execution.acctNumber(),
                    execution.execId(),
                    firstText(contract.symbol(), contract.localSymbol()),
                    firstText(contract.description(), contract.localSymbol(), contract.symbol()),
                    contract.getSecType(),
                    firstText(contract.primaryExch(), contract.exchange()),
                    contract.currency(),
                    null,
                    parseExecutionDate(execution.time()),
                    parseExecutionDateTime(execution.time()),
                    null,
                    execution.side(),
                    toBigDecimal(execution.shares()),
                    BigDecimal.valueOf(execution.price()),
                    null,
                    null,
                    null
            ));
        }

        @Override
        public void execDetailsEnd(int reqId) {
            if (reqId == requestId && finished.compareAndSet(false, true)) {
                completed.complete(List.copyOf(trades));
            }
        }

        @Override
        public void error(Exception ex) {
            fail(ex);
        }

        @Override
        public void error(String message) {
            fail(new IllegalStateException(message));
        }

        @Override
        public void error(int id, long errorTime, int errorCode, String errorMsg, String advancedOrderRejectJson) {
            if (isFatal(errorCode)) {
                fail(new IllegalStateException("TWS API error " + errorCode + ": " + errorMsg));
            }
        }

        @Override
        public void connectionClosed() {
            if (!finished.get()) {
                fail(new IllegalStateException("TWS connection closed before trade executions finished"));
            }
        }

        private BigDecimal toBigDecimal(Decimal value) {
            return value == null || !value.isValid() ? BigDecimal.ZERO : BigDecimal.valueOf(value.value().doubleValue());
        }

        private LocalDate parseExecutionDate(String value) {
            if (value == null || value.isBlank()) {
                return LocalDate.now();
            }
            try {
                return LocalDateTime.parse(value, IBKR_EXECUTION_TIME).toLocalDate();
            } catch (RuntimeException ignored) {
                return LocalDate.now();
            }
        }

        private LocalDateTime parseExecutionDateTime(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return LocalDateTime.parse(value, IBKR_EXECUTION_TIME);
            } catch (RuntimeException ignored) {
                return parseExecutionDate(value).atStartOfDay();
            }
        }

        private boolean isFatal(int errorCode) {
            return errorCode == 502
                    || errorCode == 504
                    || errorCode == 1100
                    || errorCode == 1300
                    || (errorCode >= 300 && errorCode < 400);
        }

        private String firstText(String... values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            return null;
        }
    }
}
