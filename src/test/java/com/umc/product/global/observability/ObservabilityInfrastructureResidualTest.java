package com.umc.product.global.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;

class ObservabilityInfrastructureResidualTest {

    @Test
    @DisplayName("traceparent는 sampled 여부를 보존하고 빈 값·잘못된 형식을 거부한다")
    void traceparent_capture_and_restore_edges() {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext context = mock(TraceContext.class);
        given(tracer.currentSpan()).willReturn(span);
        given(span.context()).willReturn(context);
        given(context.traceId()).willReturn("0123456789abcdef0123456789abcdef");
        given(context.spanId()).willReturn("0123456789abcdef");
        given(context.sampled()).willReturn(false);

        assertThat(W3CTraceparent.capture(tracer))
            .isEqualTo("00-0123456789abcdef0123456789abcdef-0123456789abcdef-00");
        assertThat(W3CTraceparent.restore(tracer, null)).isNull();
        assertThat(W3CTraceparent.restore(tracer, " ")).isNull();
        assertThat(W3CTraceparent.restore(tracer, "not-a-traceparent")).isNull();
    }

    @Test
    @DisplayName("Tracer bean이 없으면 span context accessor 등록을 건너뛴다")
    void span_accessor_without_tracer() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        ObjectProvider<Tracer> provider = beanFactory.getBeanProvider(Tracer.class);
        SpanContextPropagationConfig config = new SpanContextPropagationConfig(
            ObservationRegistry.NOOP,
            provider
        );

        config.registerSpanThreadLocalAccessor();

        assertThat(provider.getIfAvailable()).isNull();
    }

    @Test
    @DisplayName("ScheduledTaskTracer의 provider 생성자는 Tracer가 없어도 NOOP tracer를 사용한다")
    void scheduled_task_tracer_without_tracer_bean() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        ObjectProvider<Tracer> provider = beanFactory.getBeanProvider(Tracer.class);
        ScheduledTaskTracer tracer = new ScheduledTaskTracer(provider);

        assertThat(tracer.trace("noop", () -> { })).isNotNull();
    }
}
