package com.xavopls.ibkr_dashboard.service;

import com.xavopls.ibkr_dashboard.config.IbkrProperties;
import com.xavopls.ibkr_dashboard.dto.HistorySyncResponse;
import com.xavopls.ibkr_dashboard.dto.PnlSyncResponse;
import com.xavopls.ibkr_dashboard.dto.PositionSyncResponse;
import com.xavopls.ibkr_dashboard.dto.TradeSyncResponse;
import com.xavopls.ibkr_dashboard.entity.Account;
import com.xavopls.ibkr_dashboard.enums.Currency;
import com.xavopls.ibkr_dashboard.ibkr.IbkrAccountProfile;
import com.xavopls.ibkr_dashboard.ibkr.IbkrClient;
import com.xavopls.ibkr_dashboard.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;

@Service
public class DefaultHistorySyncService implements HistorySyncService {

    private static final Logger log = LoggerFactory.getLogger(DefaultHistorySyncService.class);

    private final IbkrProperties ibkrProperties;
    private final AccountRepository accountRepository;
    private final IbkrClient ibkrClient;
    private final TradeSyncService tradeSyncService;
    private final PnlSyncService pnlSyncService;
    private final PositionSyncService positionSyncService;

    public DefaultHistorySyncService(IbkrProperties ibkrProperties,
                                     AccountRepository accountRepository,
                                     IbkrClient ibkrClient,
                                     TradeSyncService tradeSyncService,
                                     PnlSyncService pnlSyncService,
                                     PositionSyncService positionSyncService) {
        this.ibkrProperties = ibkrProperties;
        this.accountRepository = accountRepository;
        this.ibkrClient = ibkrClient;
        this.tradeSyncService = tradeSyncService;
        this.pnlSyncService = pnlSyncService;
        this.positionSyncService = positionSyncService;
    }

    @Override
    public HistorySyncResponse syncHistory(LocalDate from, LocalDate to) {
        Account account = resolveAccount();
        LocalDate requestedFrom = from != null ? from : ibkrProperties.getHistoryFromDate();
        LocalDate resolvedTo = clampToCompletedFlexDate(to != null ? to : lastCompletedBusinessDay(LocalDate.now()));
        validateDateRange(requestedFrom, resolvedTo);
        LocalDate resolvedFrom = clampToAccountStart(account, requestedFrom, resolvedTo);

        int chunkDays = Math.max(1, ibkrProperties.getHistoryChunkDays());
        int chunks = 0;
        int fetched = 0;
        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        int delaySeconds = Math.max(0, ibkrProperties.getHistoryChunkDelaySeconds());

        log.info("Starting history sync account={} from={} to={} chunkDays={} delaySeconds={}",
                account.getAccountNumber(), resolvedFrom, resolvedTo, chunkDays, delaySeconds);

        LocalDate chunkFrom = resolvedFrom;
        while (!chunkFrom.isAfter(resolvedTo)) {
            LocalDate chunkTo = chunkFrom.plusDays(chunkDays - 1L);
            if (chunkTo.isAfter(resolvedTo)) {
                chunkTo = resolvedTo;
            }

            log.info("Syncing history trade chunk account={} from={} to={}",
                    account.getAccountNumber(), chunkFrom, chunkTo);
            TradeSyncResponse tradeSync = tradeSyncService.syncTrades(chunkFrom, chunkTo);
            fetched += tradeSync.fetched();
            inserted += tradeSync.inserted();
            updated += tradeSync.updated();
            skipped += tradeSync.skipped();
            chunks++;

            chunkFrom = chunkTo.plusDays(1);
            if (!chunkFrom.isAfter(resolvedTo) && delaySeconds > 0) {
                sleepBetweenChunks(delaySeconds);
            }
        }

        PnlSyncResponse pnl = pnlSyncService.syncPnl(resolvedFrom, resolvedTo);
        PositionSyncResponse positions = positionSyncService.syncPositions();

        log.info("Finished history sync account={} chunks={} fetched={} inserted={} updated={} skipped={} pnlUpdated={} positionsUpdated={}",
                account.getAccountNumber(), chunks, fetched, inserted, updated, skipped, pnl.updated(), positions.updated());
        return new HistorySyncResponse(
                account.getAccountNumber(),
                resolvedFrom,
                resolvedTo,
                chunks,
                fetched,
                inserted,
                updated,
                skipped,
                pnl.updated(),
                positions,
                Instant.now()
        );
    }

    private Account resolveAccount() {
        String accountId = ibkrProperties.getAccountId();
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalStateException("IBKR account id is required. Set IBKR_ACCOUNT_ID.");
        }
        return accountRepository.findByAccountNumber(accountId)
                .orElseGet(() -> accountRepository.save(new Account(accountId, accountId, Currency.USD)));
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to.");
        }
    }

    private LocalDate clampToCompletedFlexDate(LocalDate date) {
        LocalDate latestAvailableDate = lastCompletedBusinessDay(LocalDate.now());
        if (!date.isAfter(latestAvailableDate)) {
            return date;
        }

        log.warn("History sync end date {} is not a completed IBKR Flex statement day; using {} instead",
                date, latestAvailableDate);
        return latestAvailableDate;
    }

    private LocalDate clampToAccountStart(Account account, LocalDate requestedFrom, LocalDate to) {
        LocalDate profileTo = requestedFrom.plusDays(365);
        if (profileTo.isAfter(to)) {
            profileTo = to;
        }

        IbkrAccountProfile profile = ibkrClient.getAccountProfile(account.getAccountNumber(), requestedFrom, profileTo);
        LocalDate earliestAvailableDate = profile.earliestAvailableDate();
        if (earliestAvailableDate == null || !requestedFrom.isBefore(earliestAvailableDate)) {
            return requestedFrom;
        }

        log.warn("History sync start date {} is before IBKR account available date {} for account={}; using {} instead",
                requestedFrom, earliestAvailableDate, account.getAccountNumber(), earliestAvailableDate);
        return earliestAvailableDate;
    }

    private LocalDate lastCompletedBusinessDay(LocalDate date) {
        LocalDate candidate = date.minusDays(1);
        while (candidate.getDayOfWeek() == DayOfWeek.SATURDAY || candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            candidate = candidate.minusDays(1);
        }
        return candidate;
    }

    private void sleepBetweenChunks(int delaySeconds) {
        try {
            Thread.sleep(delaySeconds * 1000L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("History sync interrupted while waiting between IBKR Flex requests", ex);
        }
    }
}
