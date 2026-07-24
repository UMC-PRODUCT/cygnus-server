package com.umc.product.authentication.application.event;

import java.time.Instant;
import java.util.UUID;

import com.umc.product.global.event.domain.IntegrationEvent;

public record VerificationEmailRequestedIntegrationEvent(
    UUID eventId,
    Instant occurredAt,
    UUID requestId,
    Detail detail
) implements IntegrationEvent {

    private static final String EVENT_TYPE = "authentication.email.verification.requested.v1";
    private static final String SOURCE = "umc-product.authentication";

    public VerificationEmailRequestedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        if (requestId == null || detail == null) {
            throw new IllegalArgumentException("인증 메일 integration event 정보가 올바르지 않습니다.");
        }
    }

    public static VerificationEmailRequestedIntegrationEvent from(
        SendVerificationEmailEvent source,
        Instant expiresAt
    ) {
        return new VerificationEmailRequestedIntegrationEvent(
            null,
            source.occurredAt(),
            source.eventId(),
            new Detail(source.email(), source.verificationCode(), expiresAt.toString())
        );
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public String source() {
        return SOURCE;
    }

    public record Detail(
        String email,
        String verificationCode,
        String expiresAt
    ) {
    }
}
