package com.umc.product.global.event.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EventOutbox")
class EventOutboxTest {

    @Test
    @DisplayName("발행 실패 시 attempts를 증가시키고 다음 시도 시간을 기록한다")
    void 발행_실패_재시도() {
        EventOutbox outbox = EventOutbox.record(TestEvent.create("test.created"), "{}");
        Instant nextAttemptAt = Instant.parse("2026-05-21T00:00:10Z");

        outbox.recordFailure("temporary failure", nextAttemptAt, 3);

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(outbox.getAttempts()).isEqualTo(1);
        assertThat(outbox.getNextAttemptAt()).isEqualTo(nextAttemptAt);
        assertThat(outbox.getLastError()).isEqualTo("temporary failure");
    }

    @Test
    @DisplayName("최대 시도 횟수에 도달하면 failed 상태로 전환한다")
    void 최대_시도_횟수_도달() {
        EventOutbox outbox = EventOutbox.record(TestEvent.create("test.created"), "{}");
        Instant nextAttemptAt = Instant.parse("2026-05-21T00:00:10Z");

        outbox.recordFailure("first", nextAttemptAt, 2);
        outbox.recordFailure("second", nextAttemptAt, 2);

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.FAILED);
        assertThat(outbox.getAttempts()).isEqualTo(2);
        assertThat(outbox.getLastError()).isEqualTo("second");
    }

    private record TestEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType
    ) implements DomainEvent {

        static TestEvent create(String eventType) {
            return new TestEvent(UUID.randomUUID(), Instant.now(), eventType);
        }
    }
}
