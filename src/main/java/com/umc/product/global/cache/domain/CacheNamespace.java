package com.umc.product.global.cache.domain;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CacheNamespace {
    APPLE_JWKS("authentication.apple.jwks", "authentication.apple.jwks.l1"),
    GOOGLE_JWKS("authentication.google.jwks", "authentication.google.jwks.l1"),
    KAKAO_JWKS("authentication.kakao.jwks", "authentication.kakao.jwks.l1"),
    AUTHORITY_SNAPSHOT("authorization.authority-snapshot", "authorization.authority.snapshot");

    private final String value;
    private final String metricName;

    CacheNamespace(String value, String metricName) {
        this.value = value;
        this.metricName = metricName;
    }

    public String value() {
        return value;
    }

    public String metricName() {
        return metricName;
    }

    public static void validateUniqueValues() {
        Map<String, Long> counts = Arrays.stream(values())
            .map(CacheNamespace::value)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        validateUniqueValueCounts(counts);
    }

    static void validateUniqueValueCounts(Map<String, Long> counts) {
        counts.forEach((namespace, count) -> {
            if (count > 1) {
                throw new IllegalStateException("중복 cache namespace 입니다. namespace=" + namespace);
            }
        });
    }
}
