package com.umc.product.authentication.application.event;

import java.time.Instant;
import java.util.UUID;

import com.umc.product.global.event.domain.DomainEvent;

public record SsoBrowserLoginCreatedEvent(
    UUID eventId,
    Instant occurredAt,
    Long memberId,
    String authenticationMethod
) implements DomainEvent {

    public SsoBrowserLoginCreatedEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public static SsoBrowserLoginCreatedEvent of(Long memberId, String authenticationMethod) {
        return new SsoBrowserLoginCreatedEvent(null, null, memberId, authenticationMethod);
    }

    @Override
    public String eventType() {
        return "authentication.sso.browser-login.created";
    }
}
