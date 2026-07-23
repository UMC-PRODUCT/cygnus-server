package com.umc.product.global.event.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.global.event.application.port.in.command.RedactEventOutboxPayloadUseCase;
import com.umc.product.global.logging.OperationalMetrics;

@ExtendWith(MockitoExtension.class)
@DisplayName("Event outbox payload retention scheduler")
class EventOutboxRetentionSchedulerTest {

    @Mock
    private RedactEventOutboxPayloadUseCase redactUseCase;

    @Mock
    private OperationalMetrics operationalMetrics;

    private EventOutboxRetentionScheduler scheduler;

    @BeforeEach
    void testCase001() {
        EventOutboxRetentionProperties properties = new EventOutboxRetentionProperties(
            Duration.ofHours(24),
            Duration.ofDays(30),
            500,
            20
        );
        scheduler = new EventOutboxRetentionScheduler(redactUseCase, properties, operationalMetrics);
    }

    @Test
    @DisplayName("정리 결과를 batch metric으로 기록한다")
    void testCase002() {
        when(redactUseCase.redactExpiredPayloads(
            any(),
            eq(Duration.ofHours(24)),
            eq(Duration.ofDays(30)),
            eq(500),
            eq(20)
        )).thenReturn(7);

        scheduler.redactExpiredPayloads();

        verify(operationalMetrics).recordBatchJob(
            eq("event_outbox_payload_retention"),
            eq("success"),
            any(Duration.class),
            eq(7L)
        );
    }

    @Test
    @DisplayName("정리 실패를 metric으로 기록하고 예외를 다시 던진다")
    void testCase003() {
        IllegalStateException failure = new IllegalStateException("database unavailable");
        when(redactUseCase.redactExpiredPayloads(any(), any(), any(), eq(500), eq(20)))
            .thenThrow(failure);

        assertThatThrownBy(scheduler::redactExpiredPayloads).isSameAs(failure);
        verify(operationalMetrics).recordBatchJob(
            eq("event_outbox_payload_retention"),
            eq("failure"),
            any(Duration.class),
            eq(0L)
        );
    }
}
