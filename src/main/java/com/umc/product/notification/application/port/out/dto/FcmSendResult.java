package com.umc.product.notification.application.port.out.dto;

import java.util.List;

public record FcmSendResult(
    int successCount,
    int failureCount,
    List<Long> invalidTokenIds
) {

    public FcmSendResult {
        invalidTokenIds = invalidTokenIds == null ? List.of() : List.copyOf(invalidTokenIds);
    }

    public static FcmSendResult of(int successCount, int failureCount, List<Long> invalidTokenIds) {
        return new FcmSendResult(successCount, failureCount, invalidTokenIds);
    }
}
