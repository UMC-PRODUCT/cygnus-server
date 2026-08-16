package com.umc.product.authentication.application.event;

import java.time.Instant;
import java.util.UUID;

import com.umc.product.global.event.domain.DomainEvent;

public record SsoAuthorizationCodeIssuedEvent(
    UUID eventId,
    Instant occurredAt,
    Long memberId,
    String clientId,
    String redirectUri,
    Instant expiresAt
) implements DomainEvent {

    public SsoAuthorizationCodeIssuedEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public static SsoAuthorizationCodeIssuedEvent of(
        Long memberId,
        String clientId,
        String redirectUri,
        Instant expiresAt
    ) {
        return new SsoAuthorizationCodeIssuedEvent(null, null, memberId, clientId, redirectUri, expiresAt);
    }

    @Override
    public String eventType() {
        return "authentication.sso.authorization-code.issued";
    }
}
