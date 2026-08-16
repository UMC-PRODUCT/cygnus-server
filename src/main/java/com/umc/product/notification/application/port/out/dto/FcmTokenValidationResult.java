package com.umc.product.notification.application.port.out.dto;

import java.util.List;

public record FcmTokenValidationResult(
    int successCount,
    int failureCount,
    List<Long> validTokenIds,
    List<Long> invalidTokenIds
) {

    public FcmTokenValidationResult {
        validTokenIds = validTokenIds == null ? List.of() : List.copyOf(validTokenIds);
        invalidTokenIds = invalidTokenIds == null ? List.of() : List.copyOf(invalidTokenIds);
    }

    public static FcmTokenValidationResult of(
        int successCount,
        int failureCount,
        List<Long> validTokenIds,
        List<Long> invalidTokenIds
    ) {
        return new FcmTokenValidationResult(successCount, failureCount, validTokenIds, invalidTokenIds);
    }
}
