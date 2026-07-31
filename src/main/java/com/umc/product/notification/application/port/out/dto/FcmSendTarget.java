package com.umc.product.notification.application.port.out.dto;

public record FcmSendTarget(
    Long tokenId,
    String token
) {

    public static FcmSendTarget of(Long tokenId, String token) {
        return new FcmSendTarget(tokenId, token);
    }
}
