package com.umc.product.storage.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.storage.application.port.in.command.RunFileCleanupUseCase;
import com.umc.product.storage.application.port.in.command.dto.FileCleanupBatchResult;
import com.umc.product.storage.application.port.out.FileUsageRegistryReadinessPort;
import com.umc.product.storage.application.service.FileCleanupProperties;
import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;

@ExtendWith(MockitoExtension.class)
class OrphanFileCleanupSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    @Mock
    private RunFileCleanupUseCase cleanupUseCase;

    @Mock
    private FileUsageRegistryReadinessPort readinessPort;

    @Mock
    private OperationalMetrics operationalMetrics;

    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Test
    @DisplayName("cleanup이 disabled면 registry와 use case를 호출하지 않는다")
    void disabled면_동작하지_않는다() {
        // given
        OrphanFileCleanupScheduler sut = scheduler(properties(false));

        // when
        sut.cleanup();

        // then
        then(readinessPort).shouldHaveNoInteractions();
        then(cleanupUseCase).shouldHaveNoInteractions();
        then(operationalMetrics).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("cleanup이 enabled여도 registry가 READY가 아니면 use case를 호출하지 않는다")
    void READY가_아니면_동작하지_않는다() {
        // given
        given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.BACKFILLING);
        OrphanFileCleanupScheduler sut = scheduler(properties(true));

        // when
        sut.cleanup();

        // then
        then(cleanupUseCase).shouldHaveNoInteractions();
        then(operationalMetrics).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("READY batch 성공은 고정 job/result tag와 처리 개수로 기록한다")
    void READY_batch는_low_cardinality_metric을_기록한다() {
        // given
        given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.READY);
        given(cleanupUseCase.cleanupOrphans()).willReturn(new FileCleanupBatchResult(3, 3, 0));
        OrphanFileCleanupScheduler sut = scheduler(properties(true));

        // when
        sut.cleanup();

        // then
        then(operationalMetrics).should().recordBatchJob(
            "orphan_file_cleanup",
            "success",
            Duration.ZERO,
            3
        );
    }

    @Test
    @DisplayName("일부 claim이 retry로 전환되면 고정 retry result로 기록한다")
    void retry가_있으면_고정_retry_result를_기록한다() {
        // given
        given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.READY);
        given(cleanupUseCase.cleanupOrphans()).willReturn(new FileCleanupBatchResult(2, 1, 1));
        OrphanFileCleanupScheduler sut = scheduler(properties(true));

        // when
        sut.cleanup();

        // then
        then(operationalMetrics).should().recordBatchJob(
            "orphan_file_cleanup",
            "retry",
            Duration.ZERO,
            2
        );
    }

    @Test
    @DisplayName("예상하지 못한 batch 실패는 failure metric을 기록하고 다시 던진다")
    void batch_실패는_metric을_기록하고_다시_던진다() {
        // given
        IllegalStateException failure = new IllegalStateException("claim failed");
        given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.READY);
        given(cleanupUseCase.cleanupOrphans()).willThrow(failure);
        OrphanFileCleanupScheduler sut = scheduler(properties(true));

        // when & then
        assertThatThrownBy(sut::cleanup).isSameAs(failure);
        then(operationalMetrics).should().recordBatchJob(
            "orphan_file_cleanup",
            "failure",
            Duration.ZERO,
            0
        );
    }

    private OrphanFileCleanupScheduler scheduler(FileCleanupProperties properties) {
        return new OrphanFileCleanupScheduler(
            cleanupUseCase,
            readinessPort,
            properties,
            operationalMetrics,
            clock
        );
    }

    private FileCleanupProperties properties(boolean enabled) {
        return new FileCleanupProperties(
            enabled,
            Duration.ofMinutes(1),
            Duration.ofHours(24),
            Duration.ofHours(168),
            Duration.ofMinutes(15),
            100,
            10,
            Duration.ofMinutes(1),
            Duration.ofHours(6)
        );
    }
}
