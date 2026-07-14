package com.umc.product.audit.domain;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AuditLegacyDetailsSanitizer {

    private static final int MAX_NESTING_DEPTH = 32;
    private static final Object DROP_VALUE = new Object();

    private AuditLegacyDetailsSanitizer() {
    }

    static Map<String, Object> sanitize(Map<String, Object> details) {
        Object sanitized = sanitizeValue(details, new IdentityHashMap<>(), 0);
        if (!(sanitized instanceof Map<?, ?> map)) {
            return Map.of();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            if (key instanceof String stringKey) {
                result.put(stringKey, value);
            }
        });
        return Collections.unmodifiableMap(result);
    }

    private static Object sanitizeValue(
        Object value,
        IdentityHashMap<Object, Boolean> visiting,
        int depth
    ) {
        if (depth > MAX_NESTING_DEPTH) {
            return DROP_VALUE;
        }
        if (value instanceof Map<?, ?> map) {
            return sanitizeMap(map, visiting, depth);
        }
        if (value instanceof Iterable<?> iterable) {
            return sanitizeIterable(iterable, visiting, depth);
        }
        if (value != null && value.getClass().isArray()) {
            return sanitizeArray(value, visiting, depth);
        }
        return value;
    }

    private static Object sanitizeMap(
        Map<?, ?> map,
        IdentityHashMap<Object, Boolean> visiting,
        int depth
    ) {
        if (visiting.put(map, Boolean.TRUE) != null) {
            return DROP_VALUE;
        }
        try {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            map.forEach((key, value) -> {
                if (key instanceof String stringKey && !AuditDetailsPolicy.isForbidden(stringKey)) {
                    Object sanitizedValue = sanitizeValue(value, visiting, depth + 1);
                    if (sanitizedValue != DROP_VALUE) {
                        sanitized.put(stringKey, sanitizedValue);
                    }
                }
            });
            return Collections.unmodifiableMap(sanitized);
        } finally {
            visiting.remove(map);
        }
    }

    private static Object sanitizeIterable(
        Iterable<?> iterable,
        IdentityHashMap<Object, Boolean> visiting,
        int depth
    ) {
        if (visiting.put(iterable, Boolean.TRUE) != null) {
            return DROP_VALUE;
        }
        try {
            List<Object> sanitized = new ArrayList<>();
            iterable.forEach(value -> {
                Object sanitizedValue = sanitizeValue(value, visiting, depth + 1);
                if (sanitizedValue != DROP_VALUE) {
                    sanitized.add(sanitizedValue);
                }
            });
            return Collections.unmodifiableList(sanitized);
        } finally {
            visiting.remove(iterable);
        }
    }

    private static Object sanitizeArray(
        Object array,
        IdentityHashMap<Object, Boolean> visiting,
        int depth
    ) {
        if (visiting.put(array, Boolean.TRUE) != null) {
            return DROP_VALUE;
        }
        try {
            List<Object> sanitized = new ArrayList<>();
            for (int index = 0; index < Array.getLength(array); index++) {
                Object sanitizedValue = sanitizeValue(Array.get(array, index), visiting, depth + 1);
                if (sanitizedValue != DROP_VALUE) {
                    sanitized.add(sanitizedValue);
                }
            }
            return Collections.unmodifiableList(sanitized);
        } finally {
            visiting.remove(array);
        }
    }
}
