package com.xavopls.ibkr_dashboard.controller;

import com.xavopls.ibkr_dashboard.dto.PnlSyncResponse;
import com.xavopls.ibkr_dashboard.service.PnlSyncService;
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
public class PnlSyncController {

    private final PnlSyncService pnlSyncService;

    public PnlSyncController(PnlSyncService pnlSyncService) {
        this.pnlSyncService = pnlSyncService;
    }

    @PostMapping("/pnl")
    @Operation(
            summary = "Rebuild daily P&L",
            description = "Recomputes daily_pnl from stored trades. Currently realized P&L is derived from trades and unrealized P&L is set to zero. Dates use dd-MM-yyyy."
    )
    public PnlSyncResponse syncPnl(
            @Parameter(description = "Optional start date in dd-MM-yyyy. Defaults to earliest stored trade.")
            @RequestParam(required = false) LocalDate from,
            @Parameter(description = "Optional end date in dd-MM-yyyy. Defaults to latest stored trade.")
            @RequestParam(required = false) LocalDate to) {
        return pnlSyncService.syncPnl(from, to);
    }
}
