package com.xavopls.ibkr_dashboard.service;

import com.xavopls.ibkr_dashboard.config.IbkrProperties;
import com.xavopls.ibkr_dashboard.dto.PnlSyncResponse;
import com.xavopls.ibkr_dashboard.entity.Account;
import com.xavopls.ibkr_dashboard.entity.DailyPnl;
import com.xavopls.ibkr_dashboard.repository.AccountRepository;
import com.xavopls.ibkr_dashboard.repository.DailyPnlRepository;
import com.xavopls.ibkr_dashboard.repository.TradeRepository;
import com.xavopls.ibkr_dashboard.repository.projection.DailyRealizedPnl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DefaultPnlSyncService implements PnlSyncService {

    private static final Logger log = LoggerFactory.getLogger(DefaultPnlSyncService.class);

    private final IbkrProperties ibkrProperties;
    private final AccountRepository accountRepository;
    private final TradeRepository tradeRepository;
    private final DailyPnlRepository dailyPnlRepository;

    public DefaultPnlSyncService(IbkrProperties ibkrProperties,
                                 AccountRepository accountRepository,
                                 TradeRepository tradeRepository,
                                 DailyPnlRepository dailyPnlRepository) {
        this.ibkrProperties = ibkrProperties;
        this.accountRepository = accountRepository;
        this.tradeRepository = tradeRepository;
        this.dailyPnlRepository = dailyPnlRepository;
    }

    @Override
    @Transactional
    public PnlSyncResponse syncPnl(LocalDate from, LocalDate to) {
        validateInputDates(from, to);

        Account account = resolveAccount();
        LocalDate resolvedFrom = from != null ? from : earliestTradeDate(account);
        LocalDate resolvedTo = to != null ? to : latestTradeDate(account);
        validateDateRange(resolvedFrom, resolvedTo);
        log.info("Starting P&L sync for account={} from={} to={}",
                account.getAccountNumber(), resolvedFrom, resolvedTo);

        List<DailyRealizedPnl> dailyPnl = tradeRepository.findDailyRealizedPnl(
                account.getId(),
                resolvedFrom,
                resolvedTo
        );
        Map<LocalDate, DailyRealizedPnl> dailyPnlByDate = dailyPnl.stream()
                .collect(Collectors.toMap(DailyRealizedPnl::getDate, Function.identity()));

        long missingBasePnlRows = tradeRepository
                .countByAccountIdAndTradeDateBetweenAndRealizedPnlIsNotNullAndRealizedPnlBaseIsNull(
                        account.getId(),
                        resolvedFrom,
                        resolvedTo
                );
        if (missingBasePnlRows > 0) {
            log.warn("P&L sync ignored {} trade rows with raw realized P&L but missing base-currency P&L account={} from={} to={}",
                    missingBasePnlRows, account.getAccountNumber(), resolvedFrom, resolvedTo);
        }

        int updated = 0;
        LocalDate date = resolvedFrom;
        while (!date.isAfter(resolvedTo)) {
            DailyRealizedPnl row = dailyPnlByDate.get(date);
            BigDecimal realizedPnl = row != null && row.getRealizedPnl() != null
                    ? row.getRealizedPnl()
                    : BigDecimal.ZERO;
            upsertDailyPnl(account, date, realizedPnl);
            updated++;
            date = date.plusDays(1);
        }

        log.info("Finished P&L sync for account={} updatedDays={} missingBasePnlRows={}",
                account.getAccountNumber(), updated, missingBasePnlRows);
        return new PnlSyncResponse(account.getAccountNumber(), resolvedFrom, resolvedTo, updated, Instant.now());
    }

    private Account resolveAccount() {
        String accountId = ibkrProperties.getAccountId();
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalStateException("IBKR account id is required. Set IBKR_ACCOUNT_ID.");
        }
        return accountRepository.findByAccountNumber(accountId)
                .orElseThrow(() -> new IllegalStateException("Account must exist before P&L can be synced."));
    }

    private LocalDate earliestTradeDate(Account account) {
        return tradeRepository.findEarliestTradeDateByAccountId(account.getId())
                .orElse(LocalDate.now());
    }

    private LocalDate latestTradeDate(Account account) {
        return tradeRepository.findLatestTradeDateByAccountId(account.getId())
                .orElse(LocalDate.now());
    }

    private void validateInputDates(LocalDate from, LocalDate to) {
        if ((from == null) != (to == null)) {
            throw new IllegalArgumentException("Both from and to must be provided for a P&L sync date range.");
        }
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to.");
        }
    }

    private void upsertDailyPnl(Account account, LocalDate date, BigDecimal realizedPnl) {
        BigDecimal unrealizedPnl = BigDecimal.ZERO;
        BigDecimal totalPnl = realizedPnl.add(unrealizedPnl);

        DailyPnl dailyPnl = dailyPnlRepository.findByAccountIdAndDate(account.getId(), date)
                .orElseGet(() -> new DailyPnl(account, date, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        dailyPnl.setRealizedPnl(realizedPnl);
        dailyPnl.setUnrealizedPnl(unrealizedPnl);
        dailyPnl.setTotalPnl(totalPnl);

        dailyPnlRepository.save(dailyPnl);
    }
}
