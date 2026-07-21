package com.umc.product.notification.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.umc.product.notification.application.port.in.dto.FcmNotificationRequestInfo;

public record FcmAdminSendResponse(
    UUID requestId,
    Instant queuedAt
) {

    public static FcmAdminSendResponse from(FcmNotificationRequestInfo info) {
        return new FcmAdminSendResponse(info.requestId(), info.queuedAt());
    }
}
