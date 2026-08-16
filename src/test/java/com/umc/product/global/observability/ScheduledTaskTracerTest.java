package com.umc.product.global.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

class ScheduledTaskTracerTest {

    private Tracer tracer;
    private Span span;
    private Tracer.SpanInScope spanInScope;
    private ScheduledTaskTracer sut;

    @BeforeEach
    void setUp() {
        tracer = mock(Tracer.class);
        span = mock(Span.class);
        spanInScope = mock(Tracer.SpanInScope.class);

        given(tracer.nextSpan()).willReturn(span);
        given(tracer.withSpan(span)).willReturn(spanInScope);
        given(span.name(anyString())).willReturn(span);
        given(span.tag(anyString(), anyString())).willReturn(span);
        given(span.start()).willReturn(span);

        sut = new ScheduledTaskTracer(tracer);
    }

    @Test
    @DisplayName("동적 task를 새 root span으로 감싸 실행한다")
    void 동적_task_root_span으로_감싸_실행() {
        AtomicBoolean executed = new AtomicBoolean(false);

        Runnable traced = sut.trace("matchingRoundDeadline", () -> executed.set(true));
        traced.run();

        assertThat(executed).isTrue();
        then(tracer).should().nextSpan();
        then(span).should().name("scheduled.matchingRoundDeadline");
        then(span).should().tag("app.layer", "scheduler");
        then(span).should().tag("scheduled.task", "matchingRoundDeadline");
        then(span).should().start();
        then(span).should().end();
        then(spanInScope).should().close();
    }

    @Test
    @DisplayName("task에서 예외가 발생하면 span에 에러를 기록하고 그대로 전파한다")
    void 동적_task_예외_시_span_에러_기록() {
        RuntimeException failure = new IllegalStateException("boom");

        Runnable traced = sut.trace("matchingRoundDeadline", () -> {
            throw failure;
        });

        assertThatThrownBy(traced::run).isSameAs(failure);
        then(span).should().error(failure);
        then(span).should().end();
    }
}
