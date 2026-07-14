package com.umc.product.audit.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.OutboxDispatchMode;
import com.umc.product.global.exception.constant.Domain;

import lombok.Builder;

/**
 * 감사 로그 이벤트
 * <p>
 * AOP Aspect 또는 서비스에서 직접 발행하여 비동기로 감사 로그를 저장합니다.
 * <p>
 * {@code eventId}와 {@code occurredAt}을 명시하지 않으면 각각 {@link UUID#randomUUID()}와
 * {@link Instant#now()}로 자동 채워집니다.
 */
@Builder
public record AuditLogEvent(
    UUID eventId,
    Instant occurredAt,
    Domain domain,
    AuditAction action,
    String targetType,
    String targetId,
    Long actorMemberId,
    String description,
    Map<String, Object> details,
    String ipAddress,
    AuditOutcome outcome,
    AuditSource source,
    String requestId,
    String traceId
) implements DomainEvent {

    public AuditLogEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        if (outcome == null) {
            outcome = AuditOutcome.SUCCESS;
        }
        if (source == null) {
            source = AuditSource.ANNOTATION;
        }
    }

    @Override
    public String eventType() {
        return "audit.log." + action.name().toLowerCase();
    }

    @Override
    public OutboxDispatchMode outboxDispatchMode() {
        return OutboxDispatchMode.NON_TRANSACTIONAL;
    }
}
