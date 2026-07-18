package com.umc.product.global.event.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.global.event.application.port.in.query.dto.EventOutboxStatusInfo;
import com.umc.product.global.event.application.port.out.LoadEventOutboxPort;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxErrorCode;
import com.umc.product.global.event.domain.EventOutboxNotFoundException;

@DisplayName("EventOutboxStatusQueryService")
class EventOutboxStatusQueryServiceTest {

    private static final Instant AVAILABLE_AT = Instant.parse("2026-07-18T01:00:00.123456Z");

    @Test
    @DisplayName("eventId로 outbox 상태만 조회하고 payload 필드는 노출하지 않는다")
    void getByEventIdReturnsStatusWithoutPayload() {
        EventOutbox outbox = EventOutbox.record(
            TestEvent.create(),
            "{\"privateBusinessValue\":\"must-not-leak\"}",
            "a".repeat(64),
            AVAILABLE_AT
        );
        EventOutboxStatusQueryService service = new EventOutboxStatusQueryService(
            new FakeLoadEventOutboxPort(outbox)
        );

        EventOutboxStatusInfo info = service.getByEventId(outbox.getEventId());

        assertThat(info.eventId()).isEqualTo(outbox.getEventId());
        assertThat(info.availableAt()).isEqualTo(AVAILABLE_AT);
        assertThat(Arrays.stream(info.getClass().getRecordComponents()).map(component -> component.getName()))
            .containsExactly(
                "eventId",
                "status",
                "attempts",
                "availableAt",
                "nextAttemptAt",
                "leaseUntil",
                "failureCode",
                "publishedAt"
            );
        assertThat(info.toString()).doesNotContain("privateBusinessValue", "must-not-leak");
    }

    @Test
    @DisplayName("없는 eventId 조회는 stable not-found error를 반환한다")
    void getByEventIdMissingThrowsStableError() {
        EventOutboxStatusQueryService service = new EventOutboxStatusQueryService(
            new FakeLoadEventOutboxPort(null)
        );

        assertThatThrownBy(() -> service.getByEventId(UUID.randomUUID()))
            .isInstanceOfSatisfying(EventOutboxNotFoundException.class, exception -> {
                assertThat(exception.getBaseCode()).isEqualTo(EventOutboxErrorCode.EVENT_NOT_FOUND);
                assertThat(exception.getBaseCode().getCode()).isEqualTo("EVENT-OUTBOX-0002");
            });
    }

    @Test
    @DisplayName("상태 query service는 read-only transaction 경계를 선언한다")
    void queryServiceDeclaresReadOnlyTransaction() {
        Transactional transactional = AnnotatedElementUtils.findMergedAnnotation(
            EventOutboxStatusQueryService.class,
            Transactional.class
        );

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
    }

    private static class FakeLoadEventOutboxPort implements LoadEventOutboxPort {

        private final EventOutbox outbox;

        private FakeLoadEventOutboxPort(EventOutbox outbox) {
            this.outbox = outbox;
        }

        @Override
        public Optional<EventOutbox> findByEventId(UUID eventId) {
            if (outbox == null || !outbox.getEventId().equals(eventId)) {
                return Optional.empty();
            }
            return Optional.of(outbox);
        }

        @Override
        public List<EventOutbox> listPublishable(int limit, Instant now) {
            return List.of();
        }
    }

    private record TestEvent(UUID eventId, Instant occurredAt) implements DomainEvent {

        static TestEvent create() {
            return new TestEvent(UUID.randomUUID(), Instant.parse("2026-07-18T00:00:00Z"));
        }

        @Override
        public String eventType() {
            return "test.status.requested";
        }
    }
}
