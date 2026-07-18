package com.umc.product.global.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.umc.product.global.event.application.port.in.query.dto.EventOutboxStatusInfo;
import com.umc.product.global.event.application.port.out.dto.OutboxPublishResult;

@DisplayName("EventOutbox")
class EventOutboxTest {

    private static final String VALID_FINGERPRINT = "a".repeat(64);

    @Test
    @DisplayName("기존 record 계약은 traceparent와 초기 pending 상태를 보존한다")
    void record_기존_계약_보존() {
        TestEvent event = TestEvent.create("test.created");
        String traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

        EventOutbox outbox = EventOutbox.record(event, "{\"name\":\"test\"}", traceparent);

        assertThat(outbox.getEventId()).isEqualTo(event.eventId());
        assertThat(outbox.getEventType()).isEqualTo("test.created");
        assertThat(outbox.getEventClass()).isEqualTo(TestEvent.class.getName());
        assertThat(outbox.getPayload()).isEqualTo("{\"name\":\"test\"}");
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(outbox.getAttempts()).isZero();
        assertThat(outbox.getNextAttemptAt()).isNotNull();
        assertThat(outbox.getTraceparent()).isEqualTo(traceparent);
    }

    @Test
    @DisplayName("기존 payload 전용 record overload를 유지한다")
    void record_payload_전용_overload_보존() {
        TestEvent event = TestEvent.create("test.created");

        EventOutbox outbox = EventOutbox.record(event, "{}");

        assertThat(outbox.getTraceparent()).isNull();
    }

    @Test
    @DisplayName("멱등 예약 factory는 availableAt을 microsecond로 절삭해 초기 시각으로 기록한다")
    void recordOnce_availableAt_microsecond_절삭() {
        TestEvent event = TestEvent.create("test.created");
        Instant availableAt = Instant.parse("2026-07-18T00:00:00.123456789Z");

        EventOutbox outbox = EventOutbox.record(
            event,
            "{}",
            VALID_FINGERPRINT,
            availableAt,
            "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
        );

        Instant expected = availableAt.truncatedTo(ChronoUnit.MICROS);
        assertThat(outbox.getPayloadFingerprint()).isEqualTo(VALID_FINGERPRINT);
        assertThat(outbox.getAvailableAt()).isEqualTo(expected);
        assertThat(outbox.getNextAttemptAt()).isEqualTo(expected);
    }

    @Test
    @DisplayName("처리 시작으로 nextAttemptAt이 바뀌어도 availableAt은 바뀌지 않는다")
    void availableAt_불변() {
        Instant availableAt = Instant.parse("2026-07-18T00:00:00.123456789Z");
        EventOutbox outbox = EventOutbox.record(
            TestEvent.create("test.created"),
            "{}",
            VALID_FINGERPRINT,
            availableAt
        );
        Instant leaseUntil = Instant.parse("2026-07-18T00:05:00Z");

        outbox.markProcessing(leaseUntil);

        assertThat(outbox.getAvailableAt()).isEqualTo(availableAt.truncatedTo(ChronoUnit.MICROS));
        assertThat(outbox.getNextAttemptAt()).isEqualTo(leaseUntil);
    }

    @Test
    @DisplayName("멱등 예약 factory는 null event를 거부한다")
    void recordOnce_null_event_거부() {
        Instant availableAt = Instant.parse("2026-07-18T00:00:00Z");

        assertThatThrownBy(() -> EventOutbox.record(null, "{}", VALID_FINGERPRINT, availableAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("domain event는 필수입니다.");
    }

    @ParameterizedTest(name = "[{index}] fingerprint={0}")
    @MethodSource("invalidFingerprints")
    @DisplayName("멱등 예약 factory는 64자 소문자 16진수가 아닌 fingerprint를 거부한다")
    void recordOnce_fingerprint_검증(String fingerprint) {
        TestEvent event = TestEvent.create("test.created");
        Instant availableAt = Instant.parse("2026-07-18T00:00:00Z");

        assertThatThrownBy(() -> EventOutbox.record(event, "{}", fingerprint, availableAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("event outbox payload fingerprint는 64자리 소문자 16진수여야 합니다.");
    }

    @Test
    @DisplayName("멱등 예약 factory는 null availableAt을 거부한다")
    void recordOnce_null_availableAt_거부() {
        TestEvent event = TestEvent.create("test.created");

        assertThatThrownBy(() -> EventOutbox.record(event, "{}", VALID_FINGERPRINT, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("event outbox availableAt은 필수입니다.");
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

    @Test
    @DisplayName("pending 상태 정보는 nextAttemptAt만 노출한다")
    void statusInfo_pending_시각_분리() {
        EventOutbox outbox = EventOutbox.record(
            TestEvent.create("test.created"),
            "{}",
            VALID_FINGERPRINT,
            Instant.parse("2026-07-18T00:00:00Z")
        );

        EventOutboxStatusInfo info = EventOutboxStatusInfo.from(outbox);

        assertThat(info.nextAttemptAt()).isEqualTo(outbox.getNextAttemptAt());
        assertThat(info.leaseUntil()).isNull();
        assertThat(info.availableAt()).isEqualTo(outbox.getAvailableAt());
    }

    @Test
    @DisplayName("processing 상태 정보는 leaseUntil만 노출한다")
    void statusInfo_processing_시각_분리() {
        EventOutbox outbox = EventOutbox.record(
            TestEvent.create("test.created"),
            "{}",
            VALID_FINGERPRINT,
            Instant.parse("2026-07-18T00:00:00Z")
        );
        Instant leaseUntil = Instant.parse("2026-07-18T00:05:00Z");
        outbox.markProcessing(leaseUntil);

        EventOutboxStatusInfo info = EventOutboxStatusInfo.from(outbox);

        assertThat(info.nextAttemptAt()).isNull();
        assertThat(info.leaseUntil()).isEqualTo(leaseUntil);
    }

    @Test
    @DisplayName("종료 상태 정보는 nextAttemptAt과 leaseUntil을 노출하지 않는다")
    void statusInfo_terminal_시각_미노출() {
        EventOutbox published = EventOutbox.record(TestEvent.create("test.published"), "{}");
        EventOutbox failed = EventOutbox.record(TestEvent.create("test.failed"), "{}");
        published.markPublished();
        failed.markPublished();
        failed.recordFailure("EVENT-OUTBOX-0002", Instant.parse("2026-07-18T00:01:00Z"), 1);

        Stream<EventOutboxStatusInfo> infos = Stream.of(published, failed).map(EventOutboxStatusInfo::from);

        assertThat(infos)
            .allSatisfy(info -> {
                assertThat(info.nextAttemptAt()).isNull();
                assertThat(info.leaseUntil()).isNull();
            });
        assertThat(EventOutboxStatusInfo.from(published).publishedAt()).isEqualTo(published.getPublishedAt());
        assertThat(EventOutboxStatusInfo.from(failed).status()).isEqualTo(EventOutboxStatus.FAILED);
        assertThat(EventOutboxStatusInfo.from(failed).publishedAt()).isNull();
        assertThat(EventOutboxStatusInfo.from(failed).failureCode()).isEqualTo("EVENT-OUTBOX-0002");
    }

    @Test
    @DisplayName("상태 정보는 legacy raw 오류 문구를 failureCode로 노출하지 않는다")
    void statusInfo_legacy_raw_error_미노출() {
        EventOutbox outbox = EventOutbox.record(TestEvent.create("test.failed"), "{}");
        outbox.recordFailure("recipient=user@example.com", Instant.parse("2026-07-18T00:01:00Z"), 1);

        EventOutboxStatusInfo info = EventOutboxStatusInfo.from(outbox);

        assertThat(info.failureCode()).isNull();
    }

    @Test
    @DisplayName("발행 결과는 pending 실행 시각만 노출한다")
    void publishResult_pending_시각_노출() {
        EventOutbox outbox = EventOutbox.record(TestEvent.create("test.created"), "{}");

        OutboxPublishResult result = OutboxPublishResult.from(outbox, false);

        assertThat(result.eventId()).isEqualTo(outbox.getEventId());
        assertThat(result.status()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(result.deduplicated()).isFalse();
        assertThat(result.nextAttemptAt()).isEqualTo(outbox.getNextAttemptAt());
    }

    @Test
    @DisplayName("멱등 충돌 예외는 stable event outbox error code를 사용한다")
    void idempotencyConflict_stable_error_code() {
        OutboxIdempotencyConflictException exception = new OutboxIdempotencyConflictException();

        assertThat(exception.getBaseCode()).isEqualTo(EventOutboxErrorCode.IDEMPOTENCY_CONFLICT);
        assertThat(exception.getBaseCode().getCode()).isEqualTo("EVENT-OUTBOX-0001");
    }

    private static Stream<String> invalidFingerprints() {
        return Stream.of(
            null,
            "",
            " ",
            "a".repeat(63),
            "a".repeat(65),
            "A".repeat(64),
            "g".repeat(64)
        );
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
