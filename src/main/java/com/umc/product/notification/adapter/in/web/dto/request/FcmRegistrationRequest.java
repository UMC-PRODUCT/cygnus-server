package com.umc.product.notification.adapter.in.web.dto.request;

import com.umc.product.notification.application.port.in.dto.RegisterFcmTokenCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FcmRegistrationRequest(
    @NotBlank String fcmToken,
    @Size(max = 30) String platform,
    @Size(max = 100) String deviceId,
    @Size(max = 50) String appVersion
) {

    public RegisterFcmTokenCommand toCommand(Long memberId) {
        return RegisterFcmTokenCommand.of(memberId, fcmToken, platform, deviceId, appVersion);
    }
}
