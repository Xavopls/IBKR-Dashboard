package com.xavopls.ibkr_dashboard.ibkr;

import java.time.LocalDate;

public record IbkrAccountProfile(
        String accountId,
        LocalDate dateOpened,
        LocalDate dateFunded
) {
    public LocalDate earliestAvailableDate() {
        if (dateFunded != null) {
            return dateFunded;
        }
        return dateOpened;
    }
}
