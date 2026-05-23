package com.xavopls.ibkr_dashboard.service;

import com.xavopls.ibkr_dashboard.dto.TradeSyncResponse;

import java.time.LocalDate;

public interface TradeSyncService {

    TradeSyncResponse syncTrades(LocalDate from, LocalDate to);
}
