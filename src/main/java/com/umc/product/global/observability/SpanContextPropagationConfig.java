package com.umc.product.global.observability;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;

import io.micrometer.context.ContextRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.contextpropagation.ObservationAwareSpanThreadLocalAccessor;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 비동기 경계에서 직접 생성한 Span 컨텍스트가 전파되도록 {@link ObservationAwareSpanThreadLocalAccessor}
 * 를 전역 {@link ContextRegistry} 에 등록한다.
 *
 * <p>{@code micrometer-observation} 은 {@code ObservationThreadLocalAccessor} 만 ServiceLoader 로 자동
 * 등록하고, Spring Boot 트레이싱 오토컨피그는 Span 전용 accessor 를 등록하지 않는다. 이 accessor 가 없으면
 * {@link org.springframework.core.task.support.ContextPropagatingTaskDecorator} 가 Observation 컨텍스트만
 * 전파해, {@link TraceFlowAspect} 가 {@code tracer.withSpan()} 으로 직접 만든 Span 의 부모 연결이
 * 비동기 스레드에서 누락된다.
 *
 * <p>Observation accessor({@code micrometer.observation}) 와 Span accessor({@code micrometer.tracing}) 는
 * KEY 가 달라 공존하며, "Observation aware" accessor 는 이미 Observation 이 관리 중인 Span 은 중복 복원하지
 * 않는다. 트레이싱이 비활성(=Tracer 빈 부재)인 환경에서는 등록을 건너뛴다.
 */
@Slf4j
@Configuration
public class SpanContextPropagationConfig {

    private final ObservationRegistry observationRegistry;
    private final ObjectProvider<Tracer> tracerProvider;

    public SpanContextPropagationConfig(
        ObservationRegistry observationRegistry,
        ObjectProvider<Tracer> tracerProvider
    ) {
        this.observationRegistry = observationRegistry;
        this.tracerProvider = tracerProvider;
    }

    @PostConstruct
    void registerSpanThreadLocalAccessor() {
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer == null) {
            log.info("Tracer 빈이 없어 Span ThreadLocalAccessor 등록을 건너뜁니다 (트레이싱 비활성).");
            return;
        }
        ContextRegistry.getInstance()
            .registerThreadLocalAccessor(new ObservationAwareSpanThreadLocalAccessor(observationRegistry, tracer));
        log.info("Span ThreadLocalAccessor를 등록해 비동기 경계에서 수동 생성 Span 컨텍스트를 전파합니다.");
    }
}
