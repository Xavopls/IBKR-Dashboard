package com.xavopls.ibkr_dashboard.controller;

import com.xavopls.ibkr_dashboard.dto.TradeSyncResponse;
import com.xavopls.ibkr_dashboard.service.TradeSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/sync")
@Tag(name = "Sync", description = "Normal dashboard refresh operations")
public class TradeSyncController {

    private final TradeSyncService tradeSyncService;

    public TradeSyncController(TradeSyncService tradeSyncService) {
        this.tradeSyncService = tradeSyncService;
    }

    @PostMapping("/trades")
    @Operation(
            summary = "Sync Flex trades for a date range",
            description = "Imports historical executions from IBKR Flex for the provided range. Duplicate executions are skipped by IBKR execution id. Dates use dd-MM-yyyy."
    )
    public TradeSyncResponse syncTrades(
            @Parameter(description = "Start date in dd-MM-yyyy.")
            @RequestParam(required = false) LocalDate from,
            @Parameter(description = "End date in dd-MM-yyyy.")
            @RequestParam(required = false) LocalDate to) {
        return tradeSyncService.syncTrades(from, to);
    }
}
