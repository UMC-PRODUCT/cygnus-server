package com.umc.product.global.event.adapter.in.scheduler;

import java.time.Duration;
import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.umc.product.global.event.application.port.in.command.RedactEventOutboxPayloadUseCase;
import com.umc.product.global.logging.OperationalMetrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.event-outbox.retention",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class EventOutboxRetentionScheduler {

    private static final String JOB_NAME = "event_outbox_payload_retention";

    private final RedactEventOutboxPayloadUseCase redactEventOutboxPayloadUseCase;
    private final EventOutboxRetentionProperties properties;
    private final OperationalMetrics operationalMetrics;

    @Scheduled(fixedDelayString = "${app.event-outbox.retention.interval:PT1H}")
    public void redactExpiredPayloads() {
        Instant startedAt = Instant.now();
        try {
            int redacted = redactEventOutboxPayloadUseCase.redactExpiredPayloads(
                startedAt,
                properties.publishedPayloadRetention(),
                properties.failedPayloadRetention(),
                properties.batchSize(),
                properties.maxBatchesPerRun()
            );
            Duration duration = Duration.between(startedAt, Instant.now());
            operationalMetrics.recordBatchJob(JOB_NAME, "success", duration, redacted);
            if (redacted > 0) {
                log.info(
                    "batch job completed: jobName={}, processed={}, durationMs={}, result={}",
                    JOB_NAME,
                    redacted,
                    duration.toMillis(),
                    "success"
                );
            }
        } catch (RuntimeException exception) {
            Duration duration = Duration.between(startedAt, Instant.now());
            operationalMetrics.recordBatchJob(JOB_NAME, "failure", duration, 0);
            log.error(
                "batch job failed: jobName={}, durationMs={}, result={}, errorClass={}",
                JOB_NAME,
                duration.toMillis(),
                "failure",
                exception.getClass().getName()
            );
            throw exception;
        }
    }
}
