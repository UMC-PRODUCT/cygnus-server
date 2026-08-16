package com.umc.product.maintenance.application.service;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.maintenance.application.port.in.command.RefreshMaintenanceStateUseCase;
import com.umc.product.maintenance.application.port.out.LoadMaintenanceWindowPort;
import com.umc.product.maintenance.domain.MaintenanceSnapshot;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 점검 활성 상태의 인메모리 스냅샷. 매 요청마다 DB 를 치지 않기 위함.
 * <ul>
 *   <li>10초마다 DB 재조회 (다중 인스턴스 동기화용)</li>
 *   <li>Command 직후 동일 인스턴스에서 즉시 refresh</li>
 * </ul>
 */
@Slf4j
@Component
public class MaintenanceStateHolder implements RefreshMaintenanceStateUseCase {

    private final LoadMaintenanceWindowPort loadPort;
    private final Clock clock;
    private final AtomicReference<MaintenanceSnapshot> snapshot =
        new AtomicReference<>(MaintenanceSnapshot.none());

    @Autowired
    public MaintenanceStateHolder(LoadMaintenanceWindowPort loadPort) {
        this(loadPort, Clock.systemUTC());
    }

    MaintenanceStateHolder(LoadMaintenanceWindowPort loadPort, Clock clock) {
        this.loadPort = loadPort;
        this.clock = clock;
    }

    @PostConstruct
    void initialize() {
        refresh();
    }

    @Override
    @Transactional(readOnly = true)
    public void refresh() {
        try {
            MaintenanceSnapshot next = loadPort.findActiveAt(clock.instant())
                .map(MaintenanceSnapshot::from)
                .orElse(MaintenanceSnapshot.none());
            snapshot.set(next);
        } catch (Exception e) {
            log.warn("[MAINTENANCE] failed to refresh snapshot. keep previous.", e);
        }
    }

    public MaintenanceSnapshot current() {
        return snapshot.get();
    }
}
