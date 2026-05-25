package com.xavopls.ibkr_dashboard.service;

import com.xavopls.ibkr_dashboard.config.IbkrProperties;
import com.xavopls.ibkr_dashboard.dto.TradeSyncResponse;
import com.xavopls.ibkr_dashboard.entity.Account;
import com.xavopls.ibkr_dashboard.entity.Trade;
import com.xavopls.ibkr_dashboard.enums.Currency;
import com.xavopls.ibkr_dashboard.ibkr.IbkrClient;
import com.xavopls.ibkr_dashboard.ibkr.IbkrTradeExecution;
import com.xavopls.ibkr_dashboard.mapper.TradeExecutionMapper;
import com.xavopls.ibkr_dashboard.repository.AccountRepository;
import com.xavopls.ibkr_dashboard.repository.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class DefaultTradeSyncService implements TradeSyncService {

    private static final Logger log = LoggerFactory.getLogger(DefaultTradeSyncService.class);

    private final IbkrProperties ibkrProperties;
    private final IbkrClient ibkrClient;
    private final AccountRepository accountRepository;
    private final TradeRepository tradeRepository;
    private final TradeExecutionMapper tradeExecutionMapper;

    public DefaultTradeSyncService(IbkrProperties ibkrProperties,
                                   IbkrClient ibkrClient,
                                   AccountRepository accountRepository,
                                   TradeRepository tradeRepository,
                                   TradeExecutionMapper tradeExecutionMapper) {
        this.ibkrProperties = ibkrProperties;
        this.ibkrClient = ibkrClient;
        this.accountRepository = accountRepository;
        this.tradeRepository = tradeRepository;
        this.tradeExecutionMapper = tradeExecutionMapper;
    }

    @Override
    @Transactional
    public TradeSyncResponse syncTrades(LocalDate from, LocalDate to) {
        to = clampToCompletedFlexDate(to);
        from = nextBusinessDay(from);
        to = previousBusinessDay(to);
        validateDateRange(from, to);

        Account account = resolveAccount();
        log.info("Starting trades sync for account={} from={} to={}", account.getAccountNumber(), from, to);
        List<IbkrTradeExecution> executions = ibkrClient.getTrades(account.getAccountNumber(), from, to);
        int inserted = 0;
        int skipped = 0;
        int updated = 0;

        for (IbkrTradeExecution execution : executions) {
            if (isNotPersistableTrade(execution)) {
                skipped++;
                continue;
            }

            Trade trade = tradeExecutionMapper.toTrade(execution);
            trade.setAccount(account);

            Trade existingTrade = findExistingTrade(account, trade);
            if (existingTrade != null) {
                updateExistingTrade(existingTrade, trade);
                tradeRepository.save(existingTrade);
                updated++;
                continue;
            }

            if (naturalDuplicateExists(account, trade)) {
                skipped++;
                continue;
            }

            tradeRepository.save(trade);
            inserted++;
        }

        log.info("Finished trades sync for account={} fetched={} inserted={} updated={} skipped={}",
                account.getAccountNumber(), executions.size(), inserted, updated, skipped);
        return new TradeSyncResponse(account.getAccountNumber(), executions.size(), inserted, updated, skipped, Instant.now());
    }

    private boolean isNotPersistableTrade(IbkrTradeExecution execution) {
        return execution.quantity() == null
                || execution.price() == null
                || storedDecimal(execution.quantity()).compareTo(BigDecimal.ZERO) <= 0
                || storedDecimal(execution.price()).compareTo(BigDecimal.ZERO) <= 0;
    }

    private BigDecimal storedDecimal(BigDecimal value) {
        return value.abs().setScale(6, RoundingMode.HALF_UP);
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if ((from == null) != (to == null)) {
            throw new IllegalArgumentException("Both from and to must be provided for a trade sync date range.");
        }
        if (from != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to.");
        }
    }

    private LocalDate clampToCompletedFlexDate(LocalDate date) {
        if (date == null) {
            return null;
        }

        LocalDate latestAvailableDate = lastCompletedBusinessDay(LocalDate.now());
        if (!date.isAfter(latestAvailableDate)) {
            return date;
        }

        log.warn("Trades sync end date {} is not a completed IBKR Flex statement day; using {} instead",
                date, latestAvailableDate);
        return latestAvailableDate;
    }

    private LocalDate lastCompletedBusinessDay(LocalDate date) {
        LocalDate candidate = date.minusDays(1);
        while (isWeekend(candidate)) {
            candidate = candidate.minusDays(1);
        }
        return candidate;
    }

    private LocalDate nextBusinessDay(LocalDate date) {
        if (date == null) {
            return null;
        }

        LocalDate candidate = date;
        while (isWeekend(candidate)) {
            candidate = candidate.plusDays(1);
        }

        if (!candidate.equals(date)) {
            log.warn("Trades sync start date {} is not a business day; using {} instead", date, candidate);
        }
        return candidate;
    }

    private LocalDate previousBusinessDay(LocalDate date) {
        if (date == null) {
            return null;
        }

        LocalDate candidate = date;
        while (isWeekend(candidate)) {
            candidate = candidate.minusDays(1);
        }

        if (!candidate.equals(date)) {
            log.warn("Trades sync end date {} is not a business day; using {} instead", date, candidate);
        }
        return candidate;
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private Account resolveAccount() {
        String accountId = ibkrProperties.getAccountId();
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalStateException("IBKR account id is required. Set IBKR_ACCOUNT_ID.");
        }
        return accountRepository.findByAccountNumber(accountId)
                .orElseGet(() -> accountRepository.save(new Account(accountId, accountId, Currency.USD)));
    }

    private Trade findExistingTrade(Account account, Trade trade) {
        if (trade.getIbkrExecutionId() != null && !trade.getIbkrExecutionId().isBlank()) {
            return tradeRepository
                    .findByAccountIdAndIbkrExecutionId(account.getId(), trade.getIbkrExecutionId())
                    .orElse(null);
        }

        return null;
    }

    private boolean naturalDuplicateExists(Account account, Trade trade) {
        return tradeRepository.existsByAccountIdAndInstrumentIdAndTradeDateAndDirectionAndQuantityAndPrice(
                account.getId(),
                trade.getInstrument().getId(),
                trade.getTradeDate(),
                trade.getDirection(),
                trade.getQuantity(),
                trade.getPrice()
        );
    }

    private void updateExistingTrade(Trade existingTrade, Trade importedTrade) {
        existingTrade.setInstrument(importedTrade.getInstrument());
        existingTrade.setTradeDate(importedTrade.getTradeDate());
        existingTrade.setTradeTime(importedTrade.getTradeTime());
        existingTrade.setSettlementDate(importedTrade.getSettlementDate());
        existingTrade.setDirection(importedTrade.getDirection());
        existingTrade.setQuantity(importedTrade.getQuantity());
        existingTrade.setPrice(importedTrade.getPrice());
        existingTrade.setCommission(importedTrade.getCommission());
        existingTrade.setTradeCurrency(importedTrade.getTradeCurrency());
        existingTrade.setFxRateToBase(importedTrade.getFxRateToBase());
        existingTrade.setNetAmount(importedTrade.getNetAmount());
        existingTrade.setNetAmountBase(importedTrade.getNetAmountBase());
        existingTrade.setRealizedPnl(importedTrade.getRealizedPnl());
        existingTrade.setRealizedPnlBase(importedTrade.getRealizedPnlBase());
        existingTrade.setCommissionBase(importedTrade.getCommissionBase());
    }
}
