package com.xavopls.ibkr_dashboard.controller;

import com.xavopls.ibkr_dashboard.dto.SyncResponse;
import com.xavopls.ibkr_dashboard.service.SyncService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping
    public SyncResponse sync() {
        return syncService.sync();
    }
}
