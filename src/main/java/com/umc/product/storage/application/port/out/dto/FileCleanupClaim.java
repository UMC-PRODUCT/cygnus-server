package com.umc.product.storage.application.port.out.dto;

import java.util.Objects;
import java.util.UUID;

public record FileCleanupClaim(
    String fileId,
    String storageKey,
    UUID token,
    int attempt
) {

    public FileCleanupClaim {
        if (fileId == null || fileId.isBlank()) {
            throw new IllegalArgumentException("cleanup claim file ID는 필수입니다.");
        }
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("cleanup claim storage key는 필수입니다.");
        }
        Objects.requireNonNull(token, "cleanup claim token은 필수입니다.");
        if (attempt < 1) {
            throw new IllegalArgumentException("cleanup attempt는 1 이상이어야 합니다.");
        }
    }
}
