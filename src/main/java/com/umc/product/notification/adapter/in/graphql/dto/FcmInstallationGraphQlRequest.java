package com.umc.product.notification.adapter.in.graphql.dto;

import com.umc.product.notification.application.port.in.dto.RegisterFcmTokenCommand;

public record FcmInstallationGraphQlRequest(
    String installationId,
    String fcmToken,
    String platform,
    String appVersion
) {

    public RegisterFcmTokenCommand toCommand(Long memberId) {
        return RegisterFcmTokenCommand.of(
            memberId,
            requireText(installationId, "installationId"),
            requireText(fcmToken, "fcmToken"),
            platform,
            appVersion
        );
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "는 비어 있을 수 없습니다.");
        }
        return value;
    }
}
