package com.umc.product.notification.application.port.out.dto;

import java.util.List;

public record FcmSendResult(
    int successCount,
    int failureCount,
    List<Long> invalidTokenIds,
    List<Long> retryableTokenIds
) {

    public FcmSendResult {
        invalidTokenIds = invalidTokenIds == null ? List.of() : List.copyOf(invalidTokenIds);
        retryableTokenIds = retryableTokenIds == null ? List.of() : List.copyOf(retryableTokenIds);
    }

    public static FcmSendResult of(
        int successCount,
        int failureCount,
        List<Long> invalidTokenIds,
        List<Long> retryableTokenIds
    ) {
        return new FcmSendResult(successCount, failureCount, invalidTokenIds, retryableTokenIds);
    }
}
