package com.umc.product.llm.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.llm.adapter.out.external.LlmProperties;
import com.umc.product.llm.application.port.in.dto.ChatCompleteCommand;
import com.umc.product.llm.application.port.in.dto.ChatCompletionResult;
import com.umc.product.llm.application.port.out.ChatCompletionPort;
import com.umc.product.llm.domain.exception.LlmDomainException;
import com.umc.product.llm.domain.exception.LlmErrorCode;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@DisplayName("LLM service 잔여 엣지 케이스")
class LlmServiceResidualTest {

    @AfterEach
    void clearInterrupt() {
        Thread.interrupted();
    }

    @Test
    @DisplayName("기본 생성자는 production clock을 사용하고 활성 fallback provider gauge를 등록한다")
    void 기본_생성자와_provider_gauge를_검증한다() {
        ChatCompletionPort port = mock(ChatCompletionPort.class);
        given(port.providerName()).willReturn("mock");
        LlmMetrics metrics = new LlmMetrics(new SimpleMeterRegistry());
        ChatCompletionService sut = new ChatCompletionService(
            port,
            mock(LlmCallGuard.class),
            mock(LlmRateLimiter.class),
            metrics,
            properties("openai", new LlmProperties.RateLimit(0, 1))
        );

        sut.logActiveProvider();

        assertThat(port.providerName()).isEqualTo("mock");
    }

    @Test
    @DisplayName("provider가 구성되지 않은 비정상 wiring은 명시적인 도메인 예외로 실패한다")
    void provider_미구성을_검증한다() {
        ChatCompletionService sut = new ChatCompletionService(
            null,
            mock(LlmCallGuard.class),
            mock(LlmRateLimiter.class),
            new LlmMetrics(new SimpleMeterRegistry()),
            properties("mock", new LlmProperties.RateLimit(0, 1)),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> sut.complete(ChatCompleteCommand.freeForm(null, null)))
            .isInstanceOf(LlmDomainException.class)
            .extracting("baseCode")
            .isEqualTo(LlmErrorCode.PROVIDER_NOT_CONFIGURED);
    }

    @Test
    @DisplayName("adapter의 예상하지 못한 RuntimeException은 실패 metric·guard와 도메인 예외로 정규화한다")
    void RuntimeException을_도메인_예외로_정규화한다() {
        ChatCompletionPort port = mock(ChatCompletionPort.class);
        LlmCallGuard guard = mock(LlmCallGuard.class);
        LlmRateLimiter limiter = mock(LlmRateLimiter.class);
        given(port.providerName()).willReturn("openai");
        given(guard.allow()).willReturn(true);
        IllegalStateException failure = new IllegalStateException("unexpected");
        given(port.complete(any())).willThrow(failure);
        ChatCompletionService sut = new ChatCompletionService(
            port,
            guard,
            limiter,
            new LlmMetrics(new SimpleMeterRegistry()),
            properties("openai", new LlmProperties.RateLimit(0, 1)),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> sut.complete(ChatCompleteCommand.freeForm(null, null)))
            .isInstanceOf(LlmDomainException.class)
            .hasCause(failure)
            .extracting("baseCode")
            .isEqualTo(LlmErrorCode.CHAT_COMPLETION_FAILED);
        then(guard).should().recordFailure();
    }

    @Test
    @DisplayName("provider gauge는 null provider를 unknown으로 정규화하고 fallback 라벨을 보존한다")
    void provider_gauge의_null을_정규화한다() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        LlmMetrics sut = new LlmMetrics(registry);

        sut.registerProviderInfo(null, true);
        sut.recordTokens("mock", null, 0L);

        assertThat(registry.find("llm.active.provider.info")
            .tag("provider", "unknown")
            .tag("fallback", "true")
            .gauge()).isNotNull();
        assertThat(registry.find("llm.chat.completion.tokens.total").meters()).isEmpty();
    }

    @Test
    @DisplayName("기본 call guard 생성자는 실패 임계 전 호출을 허용한다")
    void call_guard_기본_생성자를_검증한다() {
        LlmCallGuard sut = new LlmCallGuard(properties("mock", new LlmProperties.RateLimit(0, 1)));

        assertThat(sut.allow()).isTrue();
    }

    @Test
    @DisplayName("기본 rate limiter 생성자는 비활성 설정에서 즉시 통과한다")
    void rate_limiter_기본_생성자를_검증한다() {
        LlmRateLimiter sut = new LlmRateLimiter(properties("mock", new LlmProperties.RateLimit(0, 1)));

        sut.acquire();
    }

    @Test
    @DisplayName("토큰 고갈 대기 중 interrupt는 상태를 복원하고 즉시 실패한다")
    void rate_limiter_interrupt를_처리한다() {
        LlmRateLimiter sut = new LlmRateLimiter(
            properties("mock", new LlmProperties.RateLimit(60_000, 1)),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        sut.acquire();
        Thread.currentThread().interrupt();

        assertThatThrownBy(sut::acquire)
            .isInstanceOf(IllegalStateException.class)
            .hasCauseInstanceOf(InterruptedException.class);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }

    @Test
    @DisplayName("토큰 고갈 시 계산된 최소 대기 후 리필된 토큰을 소비한다")
    void rate_limiter_최소_대기_후_리필한다() {
        Clock clock = mock(Clock.class);
        given(clock.millis()).willReturn(0L, 0L, 0L, 1L);
        LlmRateLimiter sut = new LlmRateLimiter(
            properties("mock", new LlmProperties.RateLimit(60_000, 1)), clock);

        sut.acquire();
        sut.acquire();
    }

    @Test
    @DisplayName("LLM 결과 단순 factory와 모든 예외 생성자를 보존한다")
    void 결과와_예외_생성자를_검증한다() {
        ChatCompletionResult result = ChatCompletionResult.of("answer", "mock");
        RuntimeException cause = new RuntimeException("cause");

        assertThat(result.promptTokens()).isNull();
        assertThat(new LlmDomainException(LlmErrorCode.CHAT_COMPLETION_FAILED, "message").getMessage())
            .contains("message");
        assertThat(new LlmDomainException(LlmErrorCode.CHAT_COMPLETION_FAILED, "message", cause).getCause())
            .isSameAs(cause);
        assertThat(new LlmDomainException(LlmErrorCode.CHAT_COMPLETION_FAILED, cause).getCause())
            .isSameAs(cause);
    }

    private LlmProperties properties(String provider, LlmProperties.RateLimit rateLimit) {
        return new LlmProperties(provider, "model", 0.0, 32, null, null, rateLimit);
    }
}
