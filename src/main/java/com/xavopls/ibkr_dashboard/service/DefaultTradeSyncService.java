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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class DefaultTradeSyncService implements TradeSyncService {

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
        validateDateRange(from, to);

        Account account = resolveAccount();
        List<IbkrTradeExecution> executions = ibkrClient.getTrades(account.getAccountNumber(), from, to);
        int inserted = 0;
        int skipped = 0;

        for (IbkrTradeExecution execution : executions) {
            Trade trade = tradeExecutionMapper.toTrade(execution);
            trade.setAccount(account);

            if (alreadyImported(account, trade)) {
                skipped++;
                continue;
            }

            tradeRepository.save(trade);
            inserted++;
        }

        return new TradeSyncResponse(account.getAccountNumber(), executions.size(), inserted, skipped, Instant.now());
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if ((from == null) != (to == null)) {
            throw new IllegalArgumentException("Both from and to must be provided for a trade sync date range.");
        }
        if (from != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to.");
        }
    }

    private Account resolveAccount() {
        String accountId = ibkrProperties.getAccountId();
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalStateException("IBKR account id is required. Set IBKR_ACCOUNT_ID.");
        }
        return accountRepository.findByAccountNumber(accountId)
                .orElseGet(() -> accountRepository.save(new Account(accountId, accountId, Currency.USD)));
    }

    private boolean alreadyImported(Account account, Trade trade) {
        if (trade.getIbkrExecutionId() != null && !trade.getIbkrExecutionId().isBlank()) {
            return tradeRepository.existsByAccountIdAndIbkrExecutionId(account.getId(), trade.getIbkrExecutionId());
        }

        return tradeRepository.existsByAccountIdAndInstrumentIdAndTradeDateAndDirectionAndQuantityAndPrice(
                account.getId(),
                trade.getInstrument().getId(),
                trade.getTradeDate(),
                trade.getDirection(),
                trade.getQuantity(),
                trade.getPrice()
        );
    }
}
