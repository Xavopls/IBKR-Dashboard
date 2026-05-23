package com.xavopls.ibkr_dashboard.ibkr;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record IbkrTradeExecution(
        String accountId,
        String executionId,
        String symbol,
        String name,
        String assetClass,
        String exchange,
        String currency,
        BigDecimal fxRateToBase,
        LocalDate tradeDate,
        LocalDateTime tradeTime,
        LocalDate settlementDate,
        String side,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal commission,
        BigDecimal netAmount,
        BigDecimal realizedPnl
) {
}
