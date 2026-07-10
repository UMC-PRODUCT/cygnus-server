package com.umc.product.notification.adapter.in.web.dto.request;

import com.umc.product.notification.application.port.in.dto.UnregisterFcmTokenCommand;

import jakarta.validation.constraints.NotBlank;

public record FcmUnregistrationRequest(
    @NotBlank String fcmToken
) {

    public UnregisterFcmTokenCommand toCommand(Long memberId) {
        return UnregisterFcmTokenCommand.of(memberId, fcmToken);
    }
}
