package com.umc.product.community.adapter.in.websocket;

import java.util.UUID;

public final class CommunityStompUuid {

    private CommunityStompUuid() {
    }

    public static UUID parseRequired(String value, String fieldName) {
        UUID parsed = parseOrNull(value);
        if (parsed == null) {
            throw new IllegalArgumentException(fieldName + " must be a canonical lowercase UUID");
        }
        return parsed;
    }

    public static UUID parseOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            UUID parsed = UUID.fromString(value);
            return parsed.toString().equals(value) ? parsed : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
