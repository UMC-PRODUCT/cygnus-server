package com.umc.product.authentication.application.event;

import java.time.Instant;
import java.util.UUID;

import com.umc.product.global.event.domain.DomainEvent;

public record SsoAuthorizationCodeExchangedEvent(
    UUID eventId,
    Instant occurredAt,
    Long memberId,
    String clientId,
    String redirectUri
) implements DomainEvent {

    public SsoAuthorizationCodeExchangedEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public static SsoAuthorizationCodeExchangedEvent of(Long memberId, String clientId, String redirectUri) {
        return new SsoAuthorizationCodeExchangedEvent(null, null, memberId, clientId, redirectUri);
    }

    @Override
    public String eventType() {
        return "authentication.sso.authorization-code.exchanged";
    }
}
