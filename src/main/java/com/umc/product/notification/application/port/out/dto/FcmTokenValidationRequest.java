package com.umc.product.notification.application.port.out.dto;

import java.util.List;

public record FcmTokenValidationRequest(
    List<FcmSendTarget> targets
) {

    public FcmTokenValidationRequest {
        targets = targets == null ? List.of() : List.copyOf(targets);
    }

    public static FcmTokenValidationRequest of(List<FcmSendTarget> targets) {
        return new FcmTokenValidationRequest(targets);
    }
}
