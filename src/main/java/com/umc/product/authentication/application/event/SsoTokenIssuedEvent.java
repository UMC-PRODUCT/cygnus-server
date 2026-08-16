package com.umc.product.authentication.application.event;

import java.time.Instant;
import java.util.UUID;

import com.umc.product.global.event.domain.DomainEvent;

public record SsoTokenIssuedEvent(
    UUID eventId,
    Instant occurredAt,
    Long memberId,
    String clientId,
    String grantType
) implements DomainEvent {

    public SsoTokenIssuedEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public static SsoTokenIssuedEvent of(Long memberId, String clientId, String grantType) {
        return new SsoTokenIssuedEvent(null, null, memberId, clientId, grantType);
    }

    @Override
    public String eventType() {
        return "authentication.sso.token.issued";
    }
}
