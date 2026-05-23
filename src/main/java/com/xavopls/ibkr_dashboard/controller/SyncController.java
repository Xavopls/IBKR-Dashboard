package com.xavopls.ibkr_dashboard.controller;

import com.xavopls.ibkr_dashboard.dto.SyncResponse;
import com.xavopls.ibkr_dashboard.service.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sync")
@Tag(name = "Sync", description = "Normal dashboard refresh operations")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping
    @Operation(
            summary = "Sync dashboard data",
            description = "Normal Grafana action. Refreshes current positions, imports recent Flex trades from the latest stored trade date to the last completed business day, and rebuilds daily P&L for that range."
    )
    public SyncResponse sync() {
        return syncService.sync();
    }
}
