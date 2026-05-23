package com.xavopls.ibkr_dashboard.dto;

import java.time.Instant;
import java.time.LocalDate;

public record HistorySyncResponse(
        String accountId,
        LocalDate from,
        LocalDate to,
        int chunks,
        int tradesFetched,
        int tradesInserted,
        int tradesUpdated,
        int tradesSkipped,
        int pnlUpdated,
        PositionSyncResponse positions,
        Instant syncedAt
) {
}
