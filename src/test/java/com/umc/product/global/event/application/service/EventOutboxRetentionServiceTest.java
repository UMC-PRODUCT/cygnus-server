package com.umc.product.global.event.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.global.event.application.port.out.RedactEventOutboxPayloadPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("Event outbox payload retention 서비스")
class EventOutboxRetentionServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-23T00:00:00Z");

    @Mock
    private RedactEventOutboxPayloadPort redactPort;

    @Test
    @DisplayName("PUBLISHED와 FAILED를 번갈아 batch 처리하고 모두 소진되면 중단한다")
    void testCase001() {
        Instant publishedThreshold = NOW.minus(Duration.ofHours(24));
        Instant failedThreshold = NOW.minus(Duration.ofDays(30));
        when(redactPort.redactPublishedBefore(publishedThreshold, NOW, 500)).thenReturn(500, 3);
        when(redactPort.redactFailedBefore(failedThreshold, NOW, 500)).thenReturn(2);
        EventOutboxRetentionService service = new EventOutboxRetentionService(redactPort);

        int result = service.redactExpiredPayloads(
            NOW,
            Duration.ofHours(24),
            Duration.ofDays(30),
            500,
            20
        );

        assertThat(result).isEqualTo(505);
        InOrder order = inOrder(redactPort);
        order.verify(redactPort).redactPublishedBefore(publishedThreshold, NOW, 500);
        order.verify(redactPort).redactFailedBefore(failedThreshold, NOW, 500);
        order.verify(redactPort).redactPublishedBefore(publishedThreshold, NOW, 500);
        verifyNoMoreInteractions(redactPort);
    }

    @Test
    @DisplayName("실행당 최대 batch 수를 초과하지 않는다")
    void testCase002() {
        Instant publishedThreshold = NOW.minus(Duration.ofHours(24));
        Instant failedThreshold = NOW.minus(Duration.ofDays(30));
        when(redactPort.redactPublishedBefore(publishedThreshold, NOW, 500)).thenReturn(500);
        when(redactPort.redactFailedBefore(failedThreshold, NOW, 500)).thenReturn(500);
        EventOutboxRetentionService service = new EventOutboxRetentionService(redactPort);

        int result = service.redactExpiredPayloads(
            NOW,
            Duration.ofHours(24),
            Duration.ofDays(30),
            500,
            2
        );

        assertThat(result).isEqualTo(1_000);
        InOrder order = inOrder(redactPort);
        order.verify(redactPort).redactPublishedBefore(publishedThreshold, NOW, 500);
        order.verify(redactPort).redactFailedBefore(failedThreshold, NOW, 500);
        verifyNoMoreInteractions(redactPort);
    }

    @Test
    @DisplayName("실행당 최대 batch가 1이면 FAILED 기아를 막기 위해 거부한다")
    void testCase003() {
        EventOutboxRetentionService service = new EventOutboxRetentionService(redactPort);

        assertThatThrownBy(() -> service.redactExpiredPayloads(
            NOW,
            Duration.ofHours(24),
            Duration.ofDays(30),
            500,
            1
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("최대 batch 수는 2 이상");

        verifyNoMoreInteractions(redactPort);
    }
}
