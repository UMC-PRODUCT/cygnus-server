package com.umc.product.notification.application.port.in.dto;

public record RegisterFcmTokenCommand(
    Long memberId,
    String installationId,
    String fcmToken,
    String platform,
    String appVersion
) {

    public static RegisterFcmTokenCommand of(
        Long memberId,
        String installationId,
        String fcmToken,
        String platform,
        String appVersion
    ) {
        return new RegisterFcmTokenCommand(memberId, installationId, fcmToken, platform, appVersion);
    }
}
