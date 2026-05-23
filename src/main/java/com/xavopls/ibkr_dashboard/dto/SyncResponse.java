package com.xavopls.ibkr_dashboard.dto;

import java.time.Instant;
import java.time.LocalDate;

public record SyncResponse(
        String accountId,
        LocalDate tradesFrom,
        LocalDate tradesTo,
        PositionSyncResponse positions,
        TradeSyncResponse trades,
        PnlSyncResponse pnl,
        Instant syncedAt
) {
}
