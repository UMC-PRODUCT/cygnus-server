package com.umc.product.global.event.domain;

import java.time.Duration;

public final class EventOutboxRelayPolicy {

    public static final Duration PROCESSING_LEASE = Duration.ofMinutes(5);
    public static final Duration EXTERNAL_CALL_COMPLETION_MARGIN = Duration.ofSeconds(30);
    public static final Duration MAX_EXTERNAL_CALL_TIMEOUT = PROCESSING_LEASE.minus(EXTERNAL_CALL_COMPLETION_MARGIN);

    private EventOutboxRelayPolicy() {
    }
}
