package com.umc.product.global.observability;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

/**
 * {@code @Scheduled} 어노테이션을 거치지 않는 동적(프로그래밍 방식) 스케줄 task 를 새 root span 으로 감싼다.
 *
 * <p>Spring Boot 는 {@code @Scheduled} 메서드만 {@code tasks.scheduled.execution} 관측으로 감싸 준다.
 * 반면 {@link org.springframework.scheduling.TaskScheduler#schedule} 로 직접 등록되는 task 는 trace
 * 컨텍스트 없이 실행돼, 그 안의 usecase/adapter 호출이 {@link TraceFlowAspect} 의 {@code nextSpan()} 마다
 * 제각각 새 trace 로 흩어진다. 등록 직전 Runnable 을 이 클래스로 감싸면 실행 시 새 root span 이 열려
 * 한 번의 실행 전체가 하나의 traceId 로 묶인다.
 */
@Component
public class ScheduledTaskTracer {

    private final Tracer tracer;

    @Autowired
    public ScheduledTaskTracer(ObjectProvider<Tracer> tracerProvider) {
        this(tracerProvider.getIfAvailable(() -> Tracer.NOOP));
    }

    public ScheduledTaskTracer(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * 주어진 Runnable 을 새 root span 으로 감싼 Runnable 을 돌려준다.
     *
     * <p>동적 스케줄러 스레드에는 현재 span 이 없으므로 {@code nextSpan()} 은 부모 없는 새 root span
     * (= 새 trace)이 된다.
     *
     * @param taskName span 이름·태그에 쓰일 task 식별자 (집계를 위해 저-cardinality 값 권장)
     * @param task     실제 실행할 작업
     */
    public Runnable trace(String taskName, Runnable task) {
        return () -> {
            Span span = tracer.nextSpan()
                .name("scheduled." + taskName)
                .tag("app.layer", "scheduler")
                .tag("scheduled.task", taskName);
            span.start();
            try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
                task.run();
            } catch (RuntimeException e) {
                ObservabilityErrorSanitizer.record(span, e);
                throw e;
            } finally {
                span.end();
            }
        };
    }
}
