package com.umc.product.notification.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.OutboxDispatchMode;

public record TemplateEmailRequestedEvent(
    UUID eventId,
    Instant occurredAt,
    String recipient,
    EmailTemplateType type,
    Map<String, String> variables
) implements DomainEvent {

    private static final String EVENT_TYPE = "notification.email.template.requested";

    public TemplateEmailRequestedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(variables, "variables must not be null");
        variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables));
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public OutboxDispatchMode outboxDispatchMode() {
        return OutboxDispatchMode.NON_TRANSACTIONAL;
    }
}
