package com.umc.product.recruiting.application.event;

import java.time.Instant;
import java.util.UUID;

import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.OutboxDispatchMode;

public record InterviewAvailabilityRequestedEvent(
    UUID eventId,
    Instant occurredAt,
    Long applicationId
) implements DomainEvent {

    public InterviewAvailabilityRequestedEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        if (applicationId == null || applicationId <= 0) {
            throw new IllegalArgumentException("면접 가능 일정 요청 이벤트 정보가 올바르지 않습니다.");
        }
    }

    public static InterviewAvailabilityRequestedEvent of(Long applicationId) {
        return new InterviewAvailabilityRequestedEvent(null, null, applicationId);
    }

    @Override
    public String eventType() {
        return "recruiting.interview.availability.requested";
    }

    @Override
    public OutboxDispatchMode outboxDispatchMode() {
        return OutboxDispatchMode.NON_TRANSACTIONAL;
    }
}
