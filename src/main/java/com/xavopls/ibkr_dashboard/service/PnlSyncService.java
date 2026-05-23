package com.xavopls.ibkr_dashboard.service;

import com.xavopls.ibkr_dashboard.dto.PnlSyncResponse;

import java.time.LocalDate;

public interface PnlSyncService {

    PnlSyncResponse syncPnl(LocalDate from, LocalDate to);
}
