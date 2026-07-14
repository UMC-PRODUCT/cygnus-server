package com.umc.product.audit.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

public final class AuditDescriptionPolicy {

    private static final String SAFE_FALLBACK = "감사 이벤트를 기록했습니다.";
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final Set<String> SENSITIVE_MARKERS = Set.of(
        "name",
        "nickname",
        "school",
        "이름",
        "닉네임",
        "학교",
        "password",
        "token",
        "authorization",
        "body",
        "content"
    );

    private AuditDescriptionPolicy() {
    }

    public static String sanitize(String description) {
        if (description == null || description.isBlank()) {
            return description;
        }
        String normalized = Normalizer.normalize(description, Normalizer.Form.NFKC);
        if (containsLineBreak(normalized) || containsSensitiveMarker(normalized)) {
            return SAFE_FALLBACK;
        }
        String singleLine = normalized.replaceAll("\\s+", " ").trim();
        return singleLine.length() <= MAX_DESCRIPTION_LENGTH
            ? singleLine
            : singleLine.substring(0, MAX_DESCRIPTION_LENGTH);
    }

    private static boolean containsLineBreak(String value) {
        return value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
    }

    private static boolean containsSensitiveMarker(String value) {
        String lowerCase = value.toLowerCase(Locale.ROOT);
        return SENSITIVE_MARKERS.stream().anyMatch(lowerCase::contains);
    }
}
