package com.xavopls.ibkr_dashboard.service;

import com.xavopls.ibkr_dashboard.config.IbkrProperties;
import com.xavopls.ibkr_dashboard.dto.PnlSyncResponse;
import com.xavopls.ibkr_dashboard.dto.PositionSyncResponse;
import com.xavopls.ibkr_dashboard.dto.SyncResponse;
import com.xavopls.ibkr_dashboard.dto.TradeSyncResponse;
import com.xavopls.ibkr_dashboard.entity.Account;
import com.xavopls.ibkr_dashboard.repository.AccountRepository;
import com.xavopls.ibkr_dashboard.repository.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;

@Service
public class DefaultSyncService implements SyncService {

    private static final Logger log = LoggerFactory.getLogger(DefaultSyncService.class);

    private final IbkrProperties ibkrProperties;
    private final AccountRepository accountRepository;
    private final TradeRepository tradeRepository;
    private final PositionSyncService positionSyncService;
    private final TradeSyncService tradeSyncService;
    private final PnlSyncService pnlSyncService;

    public DefaultSyncService(IbkrProperties ibkrProperties,
                              AccountRepository accountRepository,
                              TradeRepository tradeRepository,
                              PositionSyncService positionSyncService,
                              TradeSyncService tradeSyncService,
                              PnlSyncService pnlSyncService) {
        this.ibkrProperties = ibkrProperties;
        this.accountRepository = accountRepository;
        this.tradeRepository = tradeRepository;
        this.positionSyncService = positionSyncService;
        this.tradeSyncService = tradeSyncService;
        this.pnlSyncService = pnlSyncService;
    }

    @Override
    public SyncResponse sync() {
        log.info("Starting dashboard sync");
        PositionSyncResponse positions = positionSyncService.syncPositions();
        Account account = resolveAccount();
        LocalDate tradesTo = lastCompletedBusinessDay(LocalDate.now());
        LocalDate tradesFrom = latestTradeDate(account);
        if (tradesFrom.isAfter(tradesTo)) {
            tradesFrom = tradesTo;
        }

        TradeSyncResponse trades = tradeSyncService.syncTrades(tradesFrom, tradesTo);
        PnlSyncResponse pnl = pnlSyncService.syncPnl(tradesFrom, tradesTo);

        log.info("Finished dashboard sync account={} tradesFrom={} tradesTo={} positionsUpdated={} tradesInserted={} pnlUpdated={}",
                account.getAccountNumber(), tradesFrom, tradesTo, positions.updated(), trades.inserted(), pnl.updated());
        return new SyncResponse(account.getAccountNumber(), tradesFrom, tradesTo, positions, trades, pnl, Instant.now());
    }

    private Account resolveAccount() {
        String accountId = ibkrProperties.getAccountId();
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalStateException("IBKR account id is required. Set IBKR_ACCOUNT_ID.");
        }
        return accountRepository.findByAccountNumber(accountId)
                .orElseThrow(() -> new IllegalStateException("Account must exist before sync trades range can be resolved."));
    }

    private LocalDate latestTradeDate(Account account) {
        return tradeRepository.findLatestTradeDateByAccountId(account.getId())
                .orElse(lastCompletedBusinessDay(LocalDate.now()));
    }

    private LocalDate lastCompletedBusinessDay(LocalDate date) {
        LocalDate candidate = date.minusDays(1);
        while (candidate.getDayOfWeek() == DayOfWeek.SATURDAY || candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            candidate = candidate.minusDays(1);
        }
        return candidate;
    }
}
