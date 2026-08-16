package com.umc.product.community.application.port.in.query.thread.dto;

import java.util.HashSet;
import java.util.List;

final class ThreadQueryConstraints {

    private static final int MAX_KEYWORD_CODE_POINTS = 80;

    private ThreadQueryConstraints() {
    }

    static Long requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    static String normalizeKeyword(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.codePointCount(0, normalized.length()) > MAX_KEYWORD_CODE_POINTS) {
            throw new IllegalArgumentException("q must be at most 80 code points");
        }
        return normalized;
    }

    static List<Long> requireUniquePositiveIds(List<Long> values, String name) {
        if (values == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        if (values.stream().anyMatch(value -> value == null || value <= 0)) {
            throw new IllegalArgumentException(name + " must contain only positive IDs");
        }
        List<Long> copied = List.copyOf(values);
        if (new HashSet<>(copied).size() != copied.size()) {
            throw new IllegalArgumentException(name + " must not contain duplicates");
        }
        return copied;
    }

    static void requirePage(int offset, int limit) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
    }
}
