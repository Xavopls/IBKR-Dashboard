package com.xavopls.ibkr_dashboard.controller;

import com.xavopls.ibkr_dashboard.dto.PositionSyncResponse;
import com.xavopls.ibkr_dashboard.service.PositionSyncService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sync")
public class PositionSyncController {

    private final PositionSyncService positionSyncService;

    public PositionSyncController(PositionSyncService positionSyncService) {
        this.positionSyncService = positionSyncService;
    }

    @PostMapping("/positions")
    public PositionSyncResponse syncPositions() {
        return positionSyncService.syncPositions();
    }
}
