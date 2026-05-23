package com.xavopls.ibkr_dashboard.controller;

import com.xavopls.ibkr_dashboard.dto.PnlSyncResponse;
import com.xavopls.ibkr_dashboard.service.PnlSyncService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/sync")
public class PnlSyncController {

    private final PnlSyncService pnlSyncService;

    public PnlSyncController(PnlSyncService pnlSyncService) {
        this.pnlSyncService = pnlSyncService;
    }

    @PostMapping("/pnl")
    public PnlSyncResponse syncPnl(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return pnlSyncService.syncPnl(from, to);
    }
}
