package com.umc.product.global.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.contextpropagation.ObservationAwareSpanThreadLocalAccessor;
import io.micrometer.tracing.test.simple.SimpleTracer;

/**
 * 비동기 경계(@Async 스레드풀)에서 trace 컨텍스트가 끊기지 않는지 검증한다.
 *
 * <p>{@link SpanContextPropagationConfig} 가 등록하는 {@link ObservationAwareSpanThreadLocalAccessor} 와
 * {@link ContextPropagatingTaskDecorator} 의 조합으로, 직접 생성한 Span 의 traceId 가 작업 스레드로
 * 전파됨을 {@link SimpleTracer} 로 확인한다.
 *
 * <p>전역 정적 {@link ContextRegistry} 를 오염시키지 않도록, 테스트는 <b>로컬 ContextRegistry</b> 에만
 * accessor 를 등록한 {@link ContextSnapshotFactory} 로 데코레이터를 만든다. (전역에 등록·제거하면 이후 실행되는
 * {@code @SpringBootTest} 의 캐시된 컨텍스트가 잃어버린 accessor 때문에 비동기 전파에 실패할 수 있다.)
 */
class SpanContextPropagationTest {

    private SimpleTracer tracer;

    @BeforeEach
    void setUp() {
        tracer = new SimpleTracer();
    }

    @Test
    @DisplayName("TaskDecorator 가 장착된 Executor 는 비동기 스레드로 넘어가도 같은 traceId 를 전파한다")
    void TaskDecorator_장착_시_traceId_전파() throws InterruptedException {
        // given: 로컬 레지스트리 기반 데코레이터가 장착된 Executor 와, 현재 스레드에 시작된 Span
        ThreadPoolTaskExecutor executor = newExecutor(spanPropagatingDecorator());
        Span span = tracer.nextSpan().name("root").start();
        AtomicReference<String> propagatedTraceId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        // when: Span 이 scope 에 있는 동안 작업을 제출하고, 작업 스레드에서 현재 traceId 를 읽는다
        String expectedTraceId;
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            expectedTraceId = tracer.currentSpan().context().traceId();
            executor.execute(() -> {
                Span current = tracer.currentSpan();
                if (current != null) {
                    propagatedTraceId.set(current.context().traceId());
                }
                latch.countDown();
            });
            latch.await(2, TimeUnit.SECONDS);
        } finally {
            span.end();
            executor.shutdown();
        }

        // then: 작업 스레드가 원 요청과 동일한 traceId 를 본다
        assertThat(propagatedTraceId.get())
            .isNotNull()
            .isEqualTo(expectedTraceId);
    }

    @Test
    @DisplayName("TaskDecorator 가 없는 Executor 는 비동기 스레드로 trace 컨텍스트가 전파되지 않는다")
    void TaskDecorator_미장착_시_traceId_단절() throws InterruptedException {
        // given: 데코레이터가 없는 Executor 와, 현재 스레드에 시작된 Span
        ThreadPoolTaskExecutor executor = newExecutor(null);
        Span span = tracer.nextSpan().name("root").start();
        AtomicReference<String> propagatedTraceId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        // when: Span 이 scope 에 있는 동안 작업을 제출한다
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            executor.execute(() -> {
                Span current = tracer.currentSpan();
                if (current != null) {
                    propagatedTraceId.set(current.context().traceId());
                }
                latch.countDown();
            });
            latch.await(2, TimeUnit.SECONDS);
        } finally {
            span.end();
            executor.shutdown();
        }

        // then: 작업 스레드에는 현재 Span 이 없어 traceId 가 단절된다 (데코레이터의 효과 대조)
        assertThat(propagatedTraceId.get()).isNull();
    }

    /**
     * Span 전용 accessor 를 <b>로컬</b> ContextRegistry 에만 등록한 데코레이터를 만든다.
     * 전역 {@link ContextRegistry#getInstance()} 를 건드리지 않으므로 다른 테스트와 격리된다.
     */
    private ContextPropagatingTaskDecorator spanPropagatingDecorator() {
        ContextRegistry registry = new ContextRegistry()
            .registerThreadLocalAccessor(new ObservationAwareSpanThreadLocalAccessor(ObservationRegistry.NOOP, tracer));
        ContextSnapshotFactory factory = ContextSnapshotFactory.builder().contextRegistry(registry).build();
        return new ContextPropagatingTaskDecorator(factory);
    }

    private ThreadPoolTaskExecutor newExecutor(ContextPropagatingTaskDecorator taskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("test-async-");
        if (taskDecorator != null) {
            executor.setTaskDecorator(taskDecorator);
        }
        executor.initialize();
        return executor;
    }
}
