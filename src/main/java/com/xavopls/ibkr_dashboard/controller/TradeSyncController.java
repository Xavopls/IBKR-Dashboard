package com.xavopls.ibkr_dashboard.controller;

import com.xavopls.ibkr_dashboard.dto.TradeSyncResponse;
import com.xavopls.ibkr_dashboard.service.TradeSyncService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/sync")
public class TradeSyncController {

    private final TradeSyncService tradeSyncService;

    public TradeSyncController(TradeSyncService tradeSyncService) {
        this.tradeSyncService = tradeSyncService;
    }

    @PostMapping("/trades")
    public TradeSyncResponse syncTrades(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return tradeSyncService.syncTrades(from, to);
    }
}
