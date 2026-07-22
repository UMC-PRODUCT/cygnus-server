package com.umc.product.global.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EventOutbox")
class EventOutboxTest {

    @Test
    @DisplayName("도메인 이벤트와 payload로 pending outbox를 기록한다")
    void outbox_기록() {
        TestEvent event = TestEvent.create("test.created");

        EventOutbox outbox = EventOutbox.record(event, "{\"name\":\"test\"}");

        assertThat(outbox.getEventId()).isEqualTo(event.eventId());
        assertThat(outbox.getEventType()).isEqualTo("test.created");
        assertThat(outbox.getEventClass()).isEqualTo(TestEvent.class.getName());
        assertThat(outbox.getPayload()).isEqualTo("{\"name\":\"test\"}");
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(outbox.getAttempts()).isZero();
        assertThat(outbox.getNextAttemptAt()).isNotNull();
    }

    @Test
    @DisplayName("payload는 비어 있을 수 없다")
    void payload_검증() {
        TestEvent event = TestEvent.create("test.created");

        assertThatThrownBy(() -> EventOutbox.record(event, " "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("event outbox payload는 비어 있을 수 없습니다.");
    }

    @Test
    @DisplayName("domain event는 필수다")
    void domain_event_null_검증() {
        assertThatThrownBy(() -> EventOutbox.record(null, "{}"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("domain event는 필수입니다.");
    }

    @Test
    @DisplayName("발행 성공 시 published 상태와 시간을 기록한다")
    void 발행_성공() {
        EventOutbox outbox = EventOutbox.record(TestEvent.create("test.created"), "{}");

        outbox.markPublished();

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PUBLISHED);
        assertThat(outbox.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("처리 시작 시 processing 상태와 lease 만료 시간을 기록한다")
    void 처리_시작() {
        EventOutbox outbox = EventOutbox.record(TestEvent.create("test.created"), "{}");
        Instant leaseUntil = Instant.parse("2026-05-21T00:05:00Z");

        outbox.markProcessing(leaseUntil);

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PROCESSING);
        assertThat(outbox.getNextAttemptAt()).isEqualTo(leaseUntil);
    }

    @Test
    @DisplayName("processing lease 만료 시각은 필수다")
    void processing_lease_null_검증() {
        EventOutbox outbox = EventOutbox.record(TestEvent.create("test.created"), "{}");

        assertThatThrownBy(() -> outbox.markProcessing(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("event outbox processing leaseUntil은 필수입니다.");
    }

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
