package com.umc.product.audit.domain;

import java.text.Normalizer;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class AuditDetailsPolicy {

    private static final int MAX_STRING_LENGTH = 255;
    private static final String REDACTED_VALUE = "[REDACTED]";
    private static final Object DROP_VALUE = new Object();
    private static final Pattern SENSITIVE_VALUE_MARKER = Pattern.compile(
        "(?<![a-z0-9])(?:token|password|body|authorization|bearer|secret)(?![a-z0-9])"
    );

    private static final List<String> ACTOR_KEYS = List.of(
        "type", "memberId", "name", "nickname", "schoolName"
    );
    private static final List<String> TARGET_KEYS = List.of(
        "type", "id", "memberId", "name", "nickname", "schoolName", "status", "title",
        "roleName", "period", "term", "round", "recordType", "participantCount"
    );
    private static final List<String> CONTEXT_KEYS = List.of(
        "requestId", "traceId", "outcome", "source", "reason",
        "resourceType", "resourceId", "permission"
    );
    private static final Set<String> FORBIDDEN_KEYS = Set.of(
        "email",
        "password",
        "token",
        "accesstoken",
        "refreshtoken",
        "idtoken",
        "authorization",
        "authorizationheader",
        "providerid",
        "oauthsubject",
        "code",
        "verificationcode",
        "authcode",
        "onetimecode",
        "otpcode",
        "rawbody",
        "content",
        "body"
    );

    private AuditDetailsPolicy() {
    }

    public static Map<String, Object> sanitizeForStorage(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return Map.of();
        }
        return sanitizeVersionOne(AuditLegacyDetailsSanitizer.sanitize(details));
    }

    static Map<String, Object> sanitizeSection(Map<String, ?> values, Section section) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> normalizedValues = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            String normalizedKey = normalize(key);
            if (!FORBIDDEN_KEYS.contains(normalizedKey)) {
                normalizedValues.putIfAbsent(normalizedKey, value);
            }
        });

        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (String allowedKey : section.allowedKeys()) {
            Object value = sanitizeSnapshotValue(normalizedValues.get(normalize(allowedKey)));
            if (value != DROP_VALUE) {
                sanitized.put(allowedKey, value);
            }
        }
        return Collections.unmodifiableMap(sanitized);
    }

    private static Map<String, Object> sanitizeVersionOne(Map<String, Object> details) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        sanitized.put("schemaVersion", AuditDetails.CURRENT_SCHEMA_VERSION);
        sanitized.put("actor", sanitizeSection(section(details, "actor"), Section.ACTOR));
        sanitized.put("target", sanitizeSection(section(details, "target"), Section.TARGET));
        sanitized.put("context", sanitizeContext(details));
        sanitized.put("before", sanitizeSection(section(details, "before"), Section.STATE));
        sanitized.put("after", sanitizeSection(section(details, "after"), Section.STATE));
        return Collections.unmodifiableMap(sanitized);
    }

    private static Map<String, Object> sanitizeContext(Map<String, Object> details) {
        Map<String, Object> context = new LinkedHashMap<>(sanitizeSection(details, Section.CONTEXT));
        context.putAll(sanitizeSection(section(details, "context"), Section.CONTEXT));
        return Collections.unmodifiableMap(context);
    }

    private static Map<String, ?> section(Map<String, Object> details, String sectionName) {
        Object section = valueByNormalizedKey(details, normalize(sectionName));
        if (section instanceof Map<?, ?> map) {
            Map<String, Object> values = new LinkedHashMap<>();
            map.forEach((key, value) -> {
                if (key instanceof String stringKey) {
                    values.put(stringKey, value);
                }
            });
            return values;
        }
        return Map.of();
    }

    private static Object valueByNormalizedKey(Map<String, Object> values, String normalizedKey) {
        return values.entrySet().stream()
            .filter(entry -> normalize(entry.getKey()).equals(normalizedKey))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }

    private static Object sanitizeSnapshotValue(Object value) {
        if (value == null) {
            return DROP_VALUE;
        }
        if (value instanceof String stringValue) {
            if (stringValue.isBlank()) {
                return DROP_VALUE;
            }
            String normalized = Normalizer.normalize(stringValue, Normalizer.Form.NFKC);
            if (containsLineBreak(normalized)
                || SENSITIVE_VALUE_MARKER.matcher(normalized.toLowerCase(Locale.ROOT)).find()) {
                return REDACTED_VALUE;
            }
            return stringValue.length() <= MAX_STRING_LENGTH
                ? stringValue
                : stringValue.substring(0, MAX_STRING_LENGTH);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof Character || value instanceof UUID || value instanceof TemporalAccessor) {
            return value.toString();
        }
        return DROP_VALUE;
    }

    private static boolean containsLineBreak(String value) {
        return value.indexOf('\r') >= 0
            || value.indexOf('\n') >= 0
            || value.indexOf('\u2028') >= 0
            || value.indexOf('\u2029') >= 0;
    }

    static boolean isForbidden(String key) {
        return FORBIDDEN_KEYS.contains(normalize(key));
    }

    private static String normalize(String key) {
        if (key == null) {
            return "";
        }
        String normalized = Normalizer.normalize(key, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]", "");
    }

    enum Section {
        ACTOR(ACTOR_KEYS),
        TARGET(TARGET_KEYS),
        CONTEXT(CONTEXT_KEYS),
        STATE(TARGET_KEYS);

        private final List<String> allowedKeys;

        Section(List<String> allowedKeys) {
            this.allowedKeys = allowedKeys;
        }

        List<String> allowedKeys() {
            return allowedKeys;
        }
    }
}
