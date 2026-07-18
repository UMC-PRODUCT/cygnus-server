package com.umc.product.registry.domain;

import java.util.Arrays;

public enum RegistryName {
    STORAGE_USAGE("file-usage"),
    FORM_OWNERSHIP("form-ownership"),
    CHAT_OWNERSHIP("chat-room-ownership");

    private final String canonicalName;

    RegistryName(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public static RegistryName fromCanonicalName(String canonicalName) {
        return Arrays.stream(values())
            .filter(value -> value.canonicalName.equals(canonicalName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("알 수 없는 registry name입니다."));
    }
}
