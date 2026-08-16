package com.umc.product.global.cache.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CacheSpec")
class CacheSpecTest {

    @Test
    @DisplayName("namespace, valueType, ttl, maximumSize로 cache spec을 생성한다")
    void cache_spec_생성() {
        CacheSpec<String> spec = CacheSpec.of(
            CacheNamespace.GOOGLE_JWKS,
            String.class,
            Duration.ofMinutes(5),
            100L
        );

        assertThat(spec.namespace()).isEqualTo(CacheNamespace.GOOGLE_JWKS);
        assertThat(spec.valueType()).isEqualTo(String.class);
        assertThat(spec.ttl()).isEqualTo(Duration.ofMinutes(5));
        assertThat(spec.maximumSize()).isEqualTo(100L);
    }

    @Test
    @DisplayName("ttl은 양수여야 한다")
    void ttl_양수_검증() {
        assertThatThrownBy(() -> CacheSpec.of(
            CacheNamespace.GOOGLE_JWKS,
            String.class,
            Duration.ZERO,
            100L
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("cache ttl은 양수여야 합니다.");
    }

    @Test
    @DisplayName("maximumSize는 양수여야 한다")
    void maximumSize_양수_검증() {
        assertThatThrownBy(() -> CacheSpec.of(
            CacheNamespace.GOOGLE_JWKS,
            String.class,
            Duration.ofMinutes(5),
            0L
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("cache maximumSize는 양수여야 합니다.");
    }
}
