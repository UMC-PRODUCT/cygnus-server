package com.umc.product.notification.application.port.in.dto;

import java.time.Instant;

public record FcmTokenValidationInfo(
    int requestedCount,
    int invalidatedCount,
    Instant checkedAt
) {

    public static FcmTokenValidationInfo of(int requestedCount, int invalidatedCount, Instant checkedAt) {
        return new FcmTokenValidationInfo(requestedCount, invalidatedCount, checkedAt);
    }
}
