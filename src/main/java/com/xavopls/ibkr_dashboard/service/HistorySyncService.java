package com.xavopls.ibkr_dashboard.service;

import com.xavopls.ibkr_dashboard.dto.HistorySyncResponse;

import java.time.LocalDate;

public interface HistorySyncService {

    HistorySyncResponse syncHistory(LocalDate from, LocalDate to);
}
