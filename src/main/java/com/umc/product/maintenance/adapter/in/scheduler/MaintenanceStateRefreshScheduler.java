package com.umc.product.maintenance.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.umc.product.maintenance.application.port.in.command.RefreshMaintenanceStateUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MaintenanceStateRefreshScheduler {

    private final RefreshMaintenanceStateUseCase refreshMaintenanceStateUseCase;

    @Scheduled(fixedDelay = 10_000L)
    public void refresh() {
        refreshMaintenanceStateUseCase.refresh();
    }
}
