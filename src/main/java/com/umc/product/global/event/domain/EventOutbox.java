package com.umc.product.global.event.domain;

import java.time.Instant;
import java.util.UUID;

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

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "published_at")
    private Instant publishedAt;

    // 발행 시점(원 요청 trace 컨텍스트)의 W3C traceparent. relay span link 복원에 사용한다. 없을 수 있음.
    @Column(name = "traceparent", length = 64)
    private String traceparent;

    private EventOutbox(
        UUID eventId,
        String eventType,
        String eventClass,
        String payload,
        Instant nextAttemptAt,
        String traceparent
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventClass = eventClass;
        this.payload = payload;
        this.status = EventOutboxStatus.PENDING;
        this.attempts = 0;
        this.nextAttemptAt = nextAttemptAt;
        this.traceparent = traceparent;
    }

    public static EventOutbox record(DomainEvent event, String payload) {
        return record(event, payload, null);
    }

    public static EventOutbox record(DomainEvent event, String payload, String traceparent) {
        if (event == null) {
            throw new IllegalArgumentException("domain event는 필수입니다.");
        }
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("event outbox payload는 비어 있을 수 없습니다.");
        }
        return new EventOutbox(
            event.eventId(),
            event.eventType(),
            event.getClass().getName(),
            payload,
            Instant.now(),
            traceparent
        );
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
        this.nextAttemptAt = nextAttemptAt;
        if (this.attempts >= maxAttempts) {
            this.status = EventOutboxStatus.FAILED;
            return;
        }
        this.status = EventOutboxStatus.PENDING;
    }
}
