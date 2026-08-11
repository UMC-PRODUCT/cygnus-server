package com.umc.product.global.graphql.relay;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Relay Global Object Identification 스펙의 불투명(opaque) 전역 ID.
 * base64url("{typeName}:{rawId}") 형태로 인코딩한다.
 */
public record GlobalId(String typeName, String rawId) {

    private static final char SEPARATOR = ':';

    public static String encode(String typeName, long rawId) {
        return encode(typeName, Long.toString(rawId));
    }

    public static String encode(String typeName, String rawId) {
        String plain = typeName + SEPARATOR + rawId;
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(plain.getBytes(StandardCharsets.UTF_8));
    }

    public static GlobalId decode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        String plain;
        try {
            plain = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("id is not a valid global id: " + value, exception);
        }
        int separatorIndex = plain.indexOf(SEPARATOR);
        if (separatorIndex <= 0 || separatorIndex == plain.length() - 1) {
            throw new IllegalArgumentException("id is not a valid global id: " + value);
        }
        return new GlobalId(plain.substring(0, separatorIndex), plain.substring(separatorIndex + 1));
    }

    public static long decodeLong(String value, String expectedTypeName) {
        return decode(value).rawIdAsLong(expectedTypeName);
    }

    public static List<Long> decodeLongs(List<String> values, String expectedTypeName) {
        return values.stream()
            .map(value -> decodeLong(value, expectedTypeName))
            .collect(Collectors.collectingAndThen(
                Collectors.toCollection(LinkedHashSet::new),
                List::copyOf
            ));
    }

    public long rawIdAsLong(String expectedTypeName) {
        if (!typeName.equals(expectedTypeName)) {
            throw new IllegalArgumentException(
                "id must reference a " + expectedTypeName + " but references a " + typeName
            );
        }
        return rawIdAsLong();
    }

    public long rawIdAsLong() {
        try {
            return Long.parseLong(rawId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("id does not contain a numeric raw id", exception);
        }
    }
}
