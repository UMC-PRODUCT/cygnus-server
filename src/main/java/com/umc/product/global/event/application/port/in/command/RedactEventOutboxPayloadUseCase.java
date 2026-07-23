package com.umc.product.global.event.application.port.in.command;

import java.time.Duration;
import java.time.Instant;

public interface RedactEventOutboxPayloadUseCase {

    int redactExpiredPayloads(
        Instant now,
        Duration publishedRetention,
        Duration failedRetention,
        int batchSize,
        int maxBatches
    );
}
