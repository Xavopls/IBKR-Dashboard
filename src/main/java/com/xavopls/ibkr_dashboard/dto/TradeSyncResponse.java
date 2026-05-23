package com.xavopls.ibkr_dashboard.dto;

import java.time.Instant;

public record TradeSyncResponse(
        String accountId,
        int fetched,
        int inserted,
        int updated,
        int skipped,
        Instant syncedAt
) {
}
