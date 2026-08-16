package com.umc.product.maintenance.adapter.in.scheduler;

import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.maintenance.application.port.in.command.RefreshMaintenanceStateUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaintenanceStateRefreshScheduler")
class MaintenanceStateRefreshSchedulerTest {

    @Mock
    RefreshMaintenanceStateUseCase refreshMaintenanceStateUseCase;

    @Test
    @DisplayName("스케줄 실행 시 maintenance 상태 스냅샷을 refresh한다")
    void 스케줄_실행시_maintenance_상태를_refresh한다() {
        MaintenanceStateRefreshScheduler sut = new MaintenanceStateRefreshScheduler(refreshMaintenanceStateUseCase);

        sut.refresh();

        then(refreshMaintenanceStateUseCase).should().refresh();
    }
}
