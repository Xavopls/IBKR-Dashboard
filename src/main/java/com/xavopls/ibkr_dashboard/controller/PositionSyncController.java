package com.xavopls.ibkr_dashboard.controller;

import com.xavopls.ibkr_dashboard.dto.PositionSyncResponse;
import com.xavopls.ibkr_dashboard.service.PositionSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sync")
@Tag(name = "Sync", description = "Normal dashboard refresh operations")
public class PositionSyncController {

    private final PositionSyncService positionSyncService;

    public PositionSyncController(PositionSyncService positionSyncService) {
        this.positionSyncService = positionSyncService;
    }

    @PostMapping("/positions")
    @Operation(
            summary = "Sync current positions",
            description = "Fetches the current account portfolio snapshot from TWS and upserts one position row per account and instrument."
    )
    public PositionSyncResponse syncPositions() {
        return positionSyncService.syncPositions();
    }
}
