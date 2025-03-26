package com.irctc2.train.controller;

import com.irctc2.train.service.MaintenanceModeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SystemStatusController {

    @Autowired
    private MaintenanceModeService maintenanceModeService;

    @GetMapping("/system-status")
    public Map<String, Object> getSystemStatus() {
        boolean isMaintenance = maintenanceModeService.isMaintenance();
        return Map.of(
                "maintenance", isMaintenance,
                "message", isMaintenance ? "🚧 System is under maintenance. Please try again later." : ""
        );
    }
}
