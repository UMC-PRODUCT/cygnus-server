package com.umc.product.notification.application.port.in.dto;

public record UnregisterFcmTokenCommand(
    Long memberId,
    String installationId
) {

    public static UnregisterFcmTokenCommand of(Long memberId, String installationId) {
        return new UnregisterFcmTokenCommand(memberId, installationId);
    }
}
