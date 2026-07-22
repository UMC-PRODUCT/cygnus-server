package com.umc.product.global.cache.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

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
        assertThat(CacheNamespace.GOOGLE_JWKS.value()).isEqualTo("authentication.google.jwks");
    }

    @Test
    @DisplayName("중복 namespace count는 즉시 거부한다")
    void duplicate_namespace_count() {
        assertThatThrownBy(() -> CacheNamespace.validateUniqueValueCounts(Map.of("duplicate", 2L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("duplicate");
    }
}
