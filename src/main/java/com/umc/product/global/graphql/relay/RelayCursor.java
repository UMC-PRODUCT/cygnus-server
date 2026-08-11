package com.umc.product.global.graphql.relay;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Relay Connection 스펙의 불투명 커서. base64url("offset:{n}") 형태로 인코딩한다.
 */
public final class RelayCursor {

    private static final String PREFIX = "offset:";

    private RelayCursor() {
    }

    public static String encodeOffset(long offset) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString((PREFIX + offset).getBytes(StandardCharsets.UTF_8));
    }

    public static long decodeOffset(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            throw new IllegalArgumentException("cursor must not be blank");
        }
        String plain;
        try {
            plain = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("cursor is not a valid connection cursor: " + cursor, exception);
        }
        if (!plain.startsWith(PREFIX)) {
            throw new IllegalArgumentException("cursor is not a valid connection cursor: " + cursor);
        }
        try {
            long offset = Long.parseLong(plain.substring(PREFIX.length()));
            if (offset < 0) {
                throw new IllegalArgumentException("cursor offset must not be negative");
            }
            return offset;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("cursor is not a valid connection cursor: " + cursor, exception);
        }
    }
}
