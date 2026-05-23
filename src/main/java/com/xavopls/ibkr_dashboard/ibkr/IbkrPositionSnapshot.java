package com.xavopls.ibkr_dashboard.ibkr;

import java.math.BigDecimal;

public record IbkrPositionSnapshot(
        String accountId,
        String symbol,
        String name,
        String assetClass,
        String exchange,
        String currency,
        BigDecimal quantity,
        BigDecimal averageCost,
        BigDecimal marketPrice,
        BigDecimal unrealizedPnl
) {
}
