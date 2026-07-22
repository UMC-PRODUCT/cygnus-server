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

    @Test
    @DisplayName("namespace와 valueType은 필수다")
    void 필수값_검증() {
        assertThatThrownBy(() -> CacheSpec.of(
            null,
            String.class,
            Duration.ofMinutes(5),
            1L
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("cache namespace는 필수입니다.");

        assertThatThrownBy(() -> CacheSpec.of(
            CacheNamespace.GOOGLE_JWKS,
            null,
            Duration.ofMinutes(5),
            1L
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("cache valueType은 필수입니다.");
    }

    @Test
    @DisplayName("ttl은 null이거나 음수일 수 없다")
    void ttl_null_음수_검증() {
        assertThatThrownBy(() -> CacheSpec.of(
            CacheNamespace.GOOGLE_JWKS,
            String.class,
            null,
            1L
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("cache ttl은 양수여야 합니다.");

        assertThatThrownBy(() -> CacheSpec.of(
            CacheNamespace.GOOGLE_JWKS,
            String.class,
            Duration.ofSeconds(-1),
            1L
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("cache ttl은 양수여야 합니다.");
    }
}
