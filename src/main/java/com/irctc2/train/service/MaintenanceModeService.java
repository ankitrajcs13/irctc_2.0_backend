package com.irctc2.train.service;

import org.springframework.stereotype.Service;

@Service
public class MaintenanceModeService {

    private volatile boolean maintenance = false;

    public boolean isMaintenance() {
        return maintenance;
    }

    public void setMaintenance(boolean maintenance) {
        this.maintenance = maintenance;
    }
}
