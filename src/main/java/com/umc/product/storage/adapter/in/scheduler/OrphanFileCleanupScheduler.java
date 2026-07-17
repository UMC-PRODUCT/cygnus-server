package com.umc.product.storage.adapter.in.scheduler;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.storage.application.port.in.command.RunFileCleanupUseCase;
import com.umc.product.storage.application.port.in.command.dto.FileCleanupBatchResult;
import com.umc.product.storage.application.port.out.FileUsageRegistryReadinessPort;
import com.umc.product.storage.application.service.FileCleanupProperties;
import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanFileCleanupScheduler {

    private static final String JOB_NAME = "orphan_file_cleanup";

    private final RunFileCleanupUseCase cleanupUseCase;
    private final FileUsageRegistryReadinessPort readinessPort;
    private final FileCleanupProperties properties;
    private final OperationalMetrics operationalMetrics;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.storage.cleanup.poll-interval:PT1M}")
    public void cleanup() {
        if (!properties.enabled() || readinessPort.getStatus() != FileUsageRegistryStatus.READY) {
            return;
        }

        Instant startedAt = clock.instant();
        try {
            FileCleanupBatchResult batch = cleanupUseCase.cleanupOrphans();
            Duration duration = Duration.between(startedAt, clock.instant());
            String result = batch.retryScheduled() > 0 ? "retry" : "success";
            operationalMetrics.recordBatchJob(JOB_NAME, result, duration, batch.claimed());
            if (batch.retryScheduled() > 0) {
                log.warn(
                    "batch job completed: jobName={}, result={}, claimed={}, deleted={}, retryScheduled={}, durationMs={}",
                    JOB_NAME,
                    result,
                    batch.claimed(),
                    batch.deleted(),
                    batch.retryScheduled(),
                    duration.toMillis()
                );
            } else if (batch.claimed() > 0) {
                log.info(
                    "batch job completed: jobName={}, result={}, claimed={}, deleted={}, retryScheduled={}, durationMs={}",
                    JOB_NAME,
                    result,
                    batch.claimed(),
                    batch.deleted(),
                    batch.retryScheduled(),
                    duration.toMillis()
                );
            }
        } catch (RuntimeException exception) {
            Duration duration = Duration.between(startedAt, clock.instant());
            operationalMetrics.recordBatchJob(JOB_NAME, "failure", duration, 0);
            log.error(
                "batch job failed: jobName={}, result={}, durationMs={}, errorClass={}",
                JOB_NAME,
                "failure",
                duration.toMillis(),
                exception.getClass().getSimpleName(),
                exception
            );
            throw exception;
        }
    }
}
