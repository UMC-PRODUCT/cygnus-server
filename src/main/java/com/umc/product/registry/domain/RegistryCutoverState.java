package com.umc.product.registry.domain;

import java.time.Instant;
import java.util.Objects;

public record RegistryCutoverState(
    String registryName,
    RegistryStatus status,
    Instant verifiedAt,
    String details
) {

    public RegistryCutoverState {
        if (registryName == null || registryName.isBlank()) {
            throw new IllegalArgumentException("registry name은 필수입니다.");
        }
        Objects.requireNonNull(status, "registry status는 필수입니다.");
        details = details == null ? "" : details;
    }

    public static RegistryCutoverState disabled(String registryName) {
        return new RegistryCutoverState(registryName, RegistryStatus.DISABLED, null, "");
    }
}
