package com.umc.product.curriculum.adapter.in.scheduler;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.curriculum.application.port.in.command.AutoReleaseWorkbookUseCase;
import com.umc.product.global.logging.OperationalMetrics;

@DisplayName("WorkbookAutoReleaseScheduler")
class WorkbookAutoReleaseSchedulerTest {

    @Test
    @DisplayName("자동 배포 성공 시 처리 건수와 non-negative duration을 기록한다")
    void records_success_metric() {
        AutoReleaseWorkbookUseCase useCase = mock(AutoReleaseWorkbookUseCase.class);
        OperationalMetrics metrics = mock(OperationalMetrics.class);
        given(useCase.releaseAllDue()).willReturn(3);

        new WorkbookAutoReleaseScheduler(useCase, metrics).releaseDueWorkbooks();

        then(metrics).should().recordBatchJob(
            org.mockito.ArgumentMatchers.eq("workbook_auto_release"),
            org.mockito.ArgumentMatchers.eq("success"),
            argThat(duration -> !duration.isNegative()),
            org.mockito.ArgumentMatchers.eq(3L)
        );
    }

    @Test
    @DisplayName("자동 배포 실패를 삼키되 failure metric을 0건으로 기록한다")
    void records_failure_metric_without_rethrowing() {
        AutoReleaseWorkbookUseCase useCase = mock(AutoReleaseWorkbookUseCase.class);
        OperationalMetrics metrics = mock(OperationalMetrics.class);
        given(useCase.releaseAllDue()).willThrow(new IllegalStateException("실패"));

        new WorkbookAutoReleaseScheduler(useCase, metrics).releaseDueWorkbooks();

        then(metrics).should().recordBatchJob(
            org.mockito.ArgumentMatchers.eq("workbook_auto_release"),
            org.mockito.ArgumentMatchers.eq("failure"),
            argThat(duration -> !duration.isNegative()),
            org.mockito.ArgumentMatchers.eq(0L)
        );
    }
}
