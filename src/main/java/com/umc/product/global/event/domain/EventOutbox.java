package com.umc.product.global.event.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.regex.Pattern;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.umc.product.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "event_outbox")
public class EventOutbox extends BaseEntity {

    private static final Pattern PAYLOAD_FINGERPRINT_PATTERN = Pattern.compile("[0-9a-f]{64}");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 150)
    private String eventType;

    @Column(name = "event_class", nullable = false, length = 300)
    private String eventClass;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventOutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "payload_fingerprint", length = 64, updatable = false)
    private String payloadFingerprint;

    @Column(name = "available_at", updatable = false)
    private Instant availableAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "published_at")
    private Instant publishedAt;

    // 발행 시점(원 요청 trace 컨텍스트)의 W3C traceparent. relay span link 복원에 사용한다. 없을 수 있음.
    @Column(name = "traceparent", length = 64)
    private String traceparent;

    private EventOutbox(
        DomainEvent event,
        String payload,
        String traceparent,
        String payloadFingerprint,
        Instant availableAt,
        Instant nextAttemptAt
    ) {
        this.eventId = event.eventId();
        this.eventType = event.eventType();
        this.eventClass = event.getClass().getName();
        this.payload = payload;
        this.status = EventOutboxStatus.PENDING;
        this.attempts = 0;
        this.nextAttemptAt = nextAttemptAt;
        this.payloadFingerprint = payloadFingerprint;
        this.availableAt = availableAt;
        this.traceparent = traceparent;
    }

    public static EventOutbox record(DomainEvent event, String payload) {
        return record(event, payload, null);
    }

    public static EventOutbox record(DomainEvent event, String payload, String traceparent) {
        validateRequiredInputs(event, payload);
        return new EventOutbox(
            event,
            payload,
            traceparent,
            null,
            null,
            Instant.now()
        );
    }

    public static EventOutbox record(
        DomainEvent event,
        String payload,
        String payloadFingerprint,
        Instant availableAt
    ) {
        return record(event, payload, payloadFingerprint, availableAt, null);
    }

    public static EventOutbox record(
        DomainEvent event,
        String payload,
        String payloadFingerprint,
        Instant availableAt,
        String traceparent
    ) {
        validateRequiredInputs(event, payload);
        if (payloadFingerprint == null || !PAYLOAD_FINGERPRINT_PATTERN.matcher(payloadFingerprint).matches()) {
            throw new IllegalArgumentException(
                "event outbox payload fingerprint는 64자리 소문자 16진수여야 합니다."
            );
        }
        if (availableAt == null) {
            throw new IllegalArgumentException("event outbox availableAt은 필수입니다.");
        }
        Instant normalizedAvailableAt = availableAt.truncatedTo(ChronoUnit.MICROS);
        return new EventOutbox(
            event,
            payload,
            traceparent,
            payloadFingerprint,
            normalizedAvailableAt,
            normalizedAvailableAt
        );
    }

    private static void validateRequiredInputs(DomainEvent event, String payload) {
        if (event == null) {
            throw new IllegalArgumentException("domain event는 필수입니다.");
        }
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("event outbox payload는 비어 있을 수 없습니다.");
        }
    }

    public void markPublished() {
        this.status = EventOutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        this.lastError = null;
    }

    public void markProcessing(Instant leaseUntil) {
        if (leaseUntil == null) {
            throw new IllegalArgumentException("event outbox processing leaseUntil은 필수입니다.");
        }
        this.status = EventOutboxStatus.PROCESSING;
        this.nextAttemptAt = leaseUntil;
    }

    public void recordFailure(String errorMessage, Instant nextAttemptAt, int maxAttempts) {
        this.attempts++;
        this.lastError = errorMessage;
        this.publishedAt = null;
        this.nextAttemptAt = nextAttemptAt;
        if (this.attempts >= maxAttempts) {
            this.status = EventOutboxStatus.FAILED;
            return;
        }
        this.status = EventOutboxStatus.PENDING;
    }
}
