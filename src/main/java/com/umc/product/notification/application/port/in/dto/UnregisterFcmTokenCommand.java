package com.umc.product.notification.application.port.in.dto;

public record UnregisterFcmTokenCommand(
    Long memberId,
    String fcmToken
) {

    public static UnregisterFcmTokenCommand of(Long memberId, String fcmToken) {
        return new UnregisterFcmTokenCommand(memberId, fcmToken);
    }
}
