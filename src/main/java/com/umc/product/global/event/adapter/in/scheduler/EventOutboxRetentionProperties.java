package com.umc.product.global.event.adapter.in.scheduler;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.event-outbox.retention")
public record EventOutboxRetentionProperties(
    Duration publishedPayloadRetention,
    Duration failedPayloadRetention,
    int batchSize,
    int maxBatchesPerRun
) {

    public EventOutboxRetentionProperties {
        requirePositive(publishedPayloadRetention, "published-payload-retention");
        requirePositive(failedPayloadRetention, "failed-payload-retention");
        if (batchSize <= 0 || maxBatchesPerRun < 2) {
            throw new IllegalArgumentException(
                "event outbox retention batch size는 양수이고 "
                    + "실행당 최대 batch 수는 2 이상이어야 합니다."
            );
        }
    }

    private static void requirePositive(Duration value, String name) {
        if (value == null || value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException("app.event-outbox.retention." + name + "은 양수여야 합니다.");
        }
    }
}
