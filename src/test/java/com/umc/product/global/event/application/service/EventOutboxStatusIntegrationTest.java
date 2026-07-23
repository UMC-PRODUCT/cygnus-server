package com.umc.product.global.event.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import com.umc.product.global.event.adapter.out.persistence.EventOutboxJpaRepository;
import com.umc.product.global.event.application.port.in.query.GetEventOutboxStatusUseCase;
import com.umc.product.global.event.application.port.in.query.dto.EventOutboxStatusInfo;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxErrorCode;
import com.umc.product.global.event.domain.EventOutboxNotFoundException;
import com.umc.product.global.event.domain.EventOutboxStatus;
import com.umc.product.support.IntegrationTestSupport;

import jakarta.persistence.EntityManager;

@DisplayName("EventOutbox generic status query 통합")
@TestPropertySource(properties = "app.event-outbox.relay-enabled=false")
class EventOutboxStatusIntegrationTest extends IntegrationTestSupport {

    private static final Instant AVAILABLE_AT = Instant.parse("2026-07-18T01:00:00.123456Z");

    @Autowired
    private DomainEventPublisher publisher;

    @Autowired
    private GetEventOutboxStatusUseCase getEventOutboxStatusUseCase;

    @Autowired
    private EventOutboxJpaRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("모든 lifecycle 상태를 eventId로 조회하며 상태별 시각만 노출한다")
    void queryEveryLifecycleStatus() {
        UUID pendingId = UUID.fromString("40000000-0000-0000-0000-000000000001");
        UUID processingId = UUID.fromString("40000000-0000-0000-0000-000000000002");
        UUID publishedId = UUID.fromString("40000000-0000-0000-0000-000000000003");
        UUID failedId = UUID.fromString("40000000-0000-0000-0000-000000000004");
        publish(pendingId);
        publish(processingId);
        publish(publishedId);
        publish(failedId);
        Instant leaseUntil = Instant.parse("2026-07-18T01:05:00Z");
        EventOutbox processing = repository.findByEventId(processingId).orElseThrow();
        processing.markProcessing(leaseUntil);
        repository.saveAndFlush(processing);
        EventOutbox published = repository.findByEventId(publishedId).orElseThrow();
        published.markPublished();
        repository.saveAndFlush(published);
        EventOutbox failed = repository.findByEventId(failedId).orElseThrow();
        failed.recordSanitizedFailure("EMAIL-DELIVERY-0001", AVAILABLE_AT.plusSeconds(60), 1);
        repository.saveAndFlush(failed);

        EventOutboxStatusInfo pendingInfo = getEventOutboxStatusUseCase.getByEventId(pendingId);
        EventOutboxStatusInfo processingInfo = getEventOutboxStatusUseCase.getByEventId(processingId);
        EventOutboxStatusInfo publishedInfo = getEventOutboxStatusUseCase.getByEventId(publishedId);
        EventOutboxStatusInfo failedInfo = getEventOutboxStatusUseCase.getByEventId(failedId);

        assertThat(pendingInfo.status()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(pendingInfo.availableAt()).isEqualTo(AVAILABLE_AT);
        assertThat(pendingInfo.nextAttemptAt()).isEqualTo(AVAILABLE_AT);
        assertThat(pendingInfo.leaseUntil()).isNull();
        assertThat(processingInfo.status()).isEqualTo(EventOutboxStatus.PROCESSING);
        assertThat(processingInfo.nextAttemptAt()).isNull();
        assertThat(processingInfo.leaseUntil()).isEqualTo(leaseUntil);
        assertThat(publishedInfo.status()).isEqualTo(EventOutboxStatus.PUBLISHED);
        assertThat(publishedInfo.nextAttemptAt()).isNull();
        assertThat(publishedInfo.leaseUntil()).isNull();
        assertThat(publishedInfo.publishedAt()).isNotNull();
        assertThat(failedInfo.status()).isEqualTo(EventOutboxStatus.FAILED);
        assertThat(failedInfo.attempts()).isOne();
        assertThat(failedInfo.nextAttemptAt()).isNull();
        assertThat(failedInfo.leaseUntil()).isNull();
        assertThat(failedInfo.failureCode()).isEqualTo("EMAIL-DELIVERY-0001");

        jdbcTemplate.update(
            "UPDATE event_outbox SET last_error = ? WHERE event_id = ?",
            "ApplicantNameException",
            failedId
        );
        entityManager.clear();

        EventOutboxStatusInfo mixedVersionInfo = getEventOutboxStatusUseCase.getByEventId(failedId);
        assertThat(mixedVersionInfo.failureCode()).isNull();
    }

    @Test
    @DisplayName("status 응답 직렬화에는 payload와 business marker가 없다")
    void statusSerializationDoesNotExposePayload() throws Exception {
        UUID eventId = UUID.fromString("40000000-0000-0000-0000-000000000005");
        publish(eventId);

        EventOutboxStatusInfo info = getEventOutboxStatusUseCase.getByEventId(eventId);
        String serialized = objectMapper.writeValueAsString(info);

        assertThat(serialized).doesNotContain(
            "private-business-marker",
            "payload",
            "eventClass",
            "eventType",
            "payloadFingerprint"
        );
        assertThat(serialized).contains("\"eventId\"", "\"status\"", "\"availableAt\"");
    }

    @Test
    @DisplayName("없는 eventId 상태 조회는 stable not-found error를 반환한다")
    void missingEventReturnsStableNotFound() {
        assertThatThrownBy(() -> getEventOutboxStatusUseCase.getByEventId(UUID.randomUUID()))
            .isInstanceOfSatisfying(EventOutboxNotFoundException.class, exception -> {
                assertThat(exception.getBaseCode()).isEqualTo(EventOutboxErrorCode.EVENT_NOT_FOUND);
                assertThat(exception.getBaseCode().getCode()).isEqualTo("EVENT-OUTBOX-0002");
            });
    }

    private void publish(UUID eventId) {
        publisher.publishOnce(
            new StatusTestEvent(
                eventId,
                Instant.parse("2026-07-18T00:00:00Z"),
                "test.status.requested",
                new BusinessPayload("private-business-marker")
            ),
            AVAILABLE_AT
        );
    }

    private record BusinessPayload(String value) {
    }

    private record StatusTestEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        BusinessPayload business
    ) implements DomainEvent {
    }
}
