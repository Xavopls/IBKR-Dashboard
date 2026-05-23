package com.xavopls.ibkr_dashboard.controller;

import com.xavopls.ibkr_dashboard.dto.HistorySyncResponse;
import com.xavopls.ibkr_dashboard.service.HistorySyncService;
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
public class HistorySyncController {

    private final HistorySyncService historySyncService;

    public HistorySyncController(HistorySyncService historySyncService) {
        this.historySyncService = historySyncService;
    }

    @PostMapping("/history")
    @Operation(
            summary = "Backfill account history",
            description = "Administrative one-time backfill. Imports historical Flex trades in chunks, rebuilds daily P&L for the full range, and refreshes current positions. Dates use dd-MM-yyyy."
    )
    public HistorySyncResponse syncHistory(
            @Parameter(description = "Optional start date in dd-MM-yyyy. Defaults to IBKR_HISTORY_FROM_DATE.")
            @RequestParam(required = false) LocalDate from,
            @Parameter(description = "Optional end date in dd-MM-yyyy. Defaults to the last completed business day.")
            @RequestParam(required = false) LocalDate to) {
        return historySyncService.syncHistory(from, to);
    }
}
