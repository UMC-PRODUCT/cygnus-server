package com.umc.product.notification.application.port.in.dto;

public record RegisterFcmTokenCommand(
    Long memberId,
    String fcmToken,
    String platform,
    String deviceId,
    String appVersion
) {

    public static RegisterFcmTokenCommand of(
        Long memberId,
        String fcmToken,
        String platform,
        String deviceId,
        String appVersion
    ) {
        return new RegisterFcmTokenCommand(memberId, fcmToken, platform, deviceId, appVersion);
    }
}
