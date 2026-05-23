package com.xavopls.ibkr_dashboard.dto;

import java.time.Instant;
import java.time.LocalDate;

public record PnlSyncResponse(
        String accountId,
        LocalDate from,
        LocalDate to,
        int updated,
        Instant syncedAt
) {
}
