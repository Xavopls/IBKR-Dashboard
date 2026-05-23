package com.xavopls.ibkr_dashboard.dto;

import java.time.Instant;

public record PositionSyncResponse(
        String accountId,
        int fetched,
        int updated,
        Instant syncedAt
) {
}
