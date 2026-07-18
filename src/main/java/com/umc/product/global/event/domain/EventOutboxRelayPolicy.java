package com.umc.product.global.event.domain;

import java.time.Duration;

public final class EventOutboxRelayPolicy {

    public static final Duration PROCESSING_LEASE = Duration.ofMinutes(5);

    private EventOutboxRelayPolicy() {
    }
}
