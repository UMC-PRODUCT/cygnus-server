package com.umc.product.community.application.port.in.command.thread.dto;

import java.util.HashSet;
import java.util.List;

import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;

final class CommunityThreadCommandValidation {

    private CommunityThreadCommandValidation() {
    }

    static Long positiveId(Long value) {
        if (value == null || value <= 0) {
            throw invalidCommand();
        }
        return value;
    }

    static String requiredText(String value, int maxCodePoints) {
        if (value == null || value.isBlank()) {
            throw invalidCommand();
        }
        return optionalText(value, maxCodePoints);
    }

    static String optionalText(String value, int maxCodePoints) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.codePointCount(0, normalized.length()) > maxCodePoints) {
            throw invalidCommand();
        }
        return normalized;
    }

    static <T> T required(T value) {
        if (value == null) {
            throw invalidCommand();
        }
        return value;
    }

    static List<Long> uniqueIds(List<Long> values, boolean emptyAllowed) {
        List<Long> copied = values == null ? List.of() : List.copyOf(values);
        if ((!emptyAllowed && copied.isEmpty()) || copied.stream().anyMatch(value -> value == null || value <= 0)) {
            throw invalidCommand();
        }
        if (new HashSet<>(copied).size() != copied.size()) {
            throw invalidCommand();
        }
        return copied;
    }

    static CommunityDomainException invalidCommand() {
        return new CommunityDomainException(CommunityErrorCode.THREAD_INVALID_COMMAND);
    }
}
