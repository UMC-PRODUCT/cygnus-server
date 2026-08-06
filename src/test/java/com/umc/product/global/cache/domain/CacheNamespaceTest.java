package com.umc.product.global.cache.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CacheNamespace")
class CacheNamespaceTest {

    @Test
    @DisplayName("cache namespace 값은 중복되지 않는다")
    void cache_namespace_중복_없음() {
        assertThatCode(CacheNamespace::validateUniqueValues).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Google JWKS 캐시는 기존 Prometheus metric name을 유지한다")
    void google_jwks_cache_metric_name() {
        assertThat(CacheNamespace.GOOGLE_JWKS.metricName())
            .isEqualTo("authentication.google.jwks.l1");
    }
}
