package com.umc.product.storage.application.port.out.dto;

import java.time.Instant;
import java.util.Objects;

public record FileCleanupClaimCriteria(
    Instant now,
    Instant pendingCreatedBefore,
    Instant unreferencedBefore,
    Instant claimExpiredBefore,
    int batchSize,
    int maxAttempts
) {

    public FileCleanupClaimCriteria {
        Objects.requireNonNull(now, "cleanup 기준 시각은 필수입니다.");
        Objects.requireNonNull(pendingCreatedBefore, "pending retention 기준 시각은 필수입니다.");
        Objects.requireNonNull(unreferencedBefore, "unreferenced retention 기준 시각은 필수입니다.");
        Objects.requireNonNull(claimExpiredBefore, "claim timeout 기준 시각은 필수입니다.");
        if (batchSize < 1) {
            throw new IllegalArgumentException("cleanup batch size는 1 이상이어야 합니다.");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("cleanup max attempts는 1 이상이어야 합니다.");
        }
    }
}
