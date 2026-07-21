package com.umc.product.notification.application.port.in.dto;

import java.time.Instant;
import java.util.UUID;

public record FcmNotificationRequestInfo(
    UUID requestId,
    Instant queuedAt
) {

    public static FcmNotificationRequestInfo of(UUID requestId, Instant queuedAt) {
        return new FcmNotificationRequestInfo(requestId, queuedAt);
    }
}
