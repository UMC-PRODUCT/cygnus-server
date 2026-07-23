package com.umc.product.global.event.application.port.out;

import java.time.Instant;

public interface RedactEventOutboxPayloadPort {

    int redactPublishedBefore(Instant threshold, Instant redactedAt, int limit);

    int redactFailedBefore(Instant threshold, Instant redactedAt, int limit);
}
