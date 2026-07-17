package com.umc.product.storage.application.port.out.dto;

import java.time.Instant;
import java.util.Objects;

public record FileCleanupFailure(
    FileCleanupClaim claim,
    Instant failedAt,
    Instant nextAttemptAt,
    int maxAttempts
) {

    public FileCleanupFailure {
        Objects.requireNonNull(claim, "실패한 cleanup claim은 필수입니다.");
        Objects.requireNonNull(failedAt, "cleanup 실패 시각은 필수입니다.");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("cleanup max attempts는 1 이상이어야 합니다.");
        }
    }
}
