package com.umc.product.llm.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LlmProperties 잔여 기본값")
class LlmPropertiesResidualTest {

    @Test
    @DisplayName("null provider와 모델 설정은 운영 기본값으로 정규화한다")
    void null_설정을_기본값으로_정규화한다() {
        LlmProperties properties = new LlmProperties(null, null, null, null, null, null, null);

        assertThat(properties.provider()).isEqualTo("mock");
        assertThat(properties.model()).isEqualTo("gemini-2.5-flash-lite");
        assertThat(properties.temperature()).isZero();
        assertThat(properties.maxOutputTokens()).isEqualTo(32);
    }

    @Test
    @DisplayName("음수 RPM과 0 burst는 비활성·최소 burst 설정으로 보정한다")
    void rate_limit_경계를_보정한다() {
        LlmProperties.RateLimit disabled = new LlmProperties.RateLimit(-1, 0);
        LlmProperties.RateLimit enabled = new LlmProperties.RateLimit(2, 0);

        assertThat(disabled.requestsPerMinute()).isZero();
        assertThat(disabled.burst()).isOne();
        assertThat(disabled.enabled()).isFalse();
        assertThat(enabled.burst()).isEqualTo(2);
        assertThat(enabled.enabled()).isTrue();
    }
}
