package com.umc.product.llm.application.service;

import java.time.Duration;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * LLM 호출 관측 메트릭 등록자.
 * <p>
 * cardinality 폭증을 막기 위해 라벨은 provider, status, type 으로만 제한한다 (ADR-006).
 *
 * <ul>
 *   <li>{@code llm_chat_completion_seconds{provider, status}} – 호출 latency</li>
 *   <li>{@code llm_chat_completion_total{provider, status}} – 호출 카운트</li>
 *   <li>{@code llm_chat_completion_tokens_total{provider, type}} – 토큰 사용량 (type=in|out)</li>
 * </ul>
 */
@Component
public class LlmMetrics {

    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_CIRCUIT_OPEN = "circuit-open";

    private static final String METRIC_LATENCY = "llm.chat.completion.seconds";
    private static final String METRIC_TOTAL = "llm.chat.completion.total";
    private static final String METRIC_TOKENS = "llm.chat.completion.tokens.total";

    private static final String TAG_PROVIDER = "provider";
    private static final String TAG_STATUS = "status";
    private static final String TAG_TYPE = "type";
    private static final String TYPE_IN = "in";
    private static final String TYPE_OUT = "out";

    private final MeterRegistry registry;

    public LlmMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordCall(String provider, String status, Duration latency) {
        Timer.builder(METRIC_LATENCY)
            .tag(TAG_PROVIDER, provider)
            .tag(TAG_STATUS, status)
            .register(registry)
            .record(latency);
        Counter.builder(METRIC_TOTAL)
            .tag(TAG_PROVIDER, provider)
            .tag(TAG_STATUS, status)
            .register(registry)
            .increment();
    }

    /**
     * 활성 LLM provider 와 fallback 어댑터 진입 여부를 항상 노출되는 gauge 로 등록한다.
     * <p>
     * {@code llm_active_provider_info{provider, fallback}} 으로 노출되며 값은 항상 1. 운영자는 라벨만으로 활성 provider 와 fallback 진입 사실을 즉시
     * 확인할 수 있다.
     */
    public void registerProviderInfo(String provider, boolean fallbackEngaged) {
        Gauge.builder("llm.active.provider.info", () -> 1)
            .description("활성 LLM provider 와 fallback 어댑터 진입 여부 (항상 1, 라벨로 식별)")
            .tag(TAG_PROVIDER, provider == null ? "unknown" : provider)
            .tag("fallback", Boolean.toString(fallbackEngaged))
            .strongReference(true)
            .register(registry);
    }

    public void recordTokens(String provider, Long promptTokens, Long completionTokens) {
        if (promptTokens != null && promptTokens > 0) {
            Counter.builder(METRIC_TOKENS)
                .tag(TAG_PROVIDER, provider)
                .tag(TAG_TYPE, TYPE_IN)
                .register(registry)
                .increment(promptTokens);
        }
        if (completionTokens != null && completionTokens > 0) {
            Counter.builder(METRIC_TOKENS)
                .tag(TAG_PROVIDER, provider)
                .tag(TAG_TYPE, TYPE_OUT)
                .register(registry)
                .increment(completionTokens);
        }
    }
}
