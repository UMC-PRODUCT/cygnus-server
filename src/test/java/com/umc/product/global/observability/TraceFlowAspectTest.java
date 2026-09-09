package com.umc.product.global.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

class TraceFlowAspectTest {

    private Tracer tracer;
    private Span span;
    private Tracer.SpanInScope spanInScope;
    private TraceFlowAspect sut;

    @BeforeEach
    void setUp() {
        tracer = mock(Tracer.class);
        span = mock(Span.class);
        spanInScope = mock(Tracer.SpanInScope.class);

        given(tracer.nextSpan()).willReturn(span);
        given(tracer.withSpan(span)).willReturn(spanInScope);
        given(span.name(org.mockito.ArgumentMatchers.anyString())).willReturn(span);
        given(span.tag(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
            .willReturn(span);
        given(span.start()).willReturn(span);

        sut = new TraceFlowAspect(tracer, new ObservabilityTracingProperties());
    }

    @Test
    @DisplayName("UseCase 구현체 호출을 UseCase 이름의 span으로 감싼다")
    void usecase_구현체_호출_span_생성() throws Throwable {
        Method method = DemoUseCase.class.getMethod("getById", Long.class);
        ProceedingJoinPoint joinPoint = joinPoint(method, new DemoQueryService(), "result");

        Object result = sut.traceUseCaseAndAdapter(joinPoint);

        assertThat(result).isEqualTo("result");
        then(span).should().name("usecase.DemoUseCase.getById");
        then(span).should().tag("app.layer", "application");
        then(span).should().tag("app.domain", "demo");
        then(span).should().tag("app.usecase", "DemoUseCase");
        then(span).should().tag("code.function", "getById");
        then(span).should().end();
        then(spanInScope).should().close();
    }

    @Test
    @DisplayName("adapter.out 호출을 adapter span으로 감싼다")
    void adapter_out_호출_span_생성() throws Throwable {
        Method method = DemoPersistenceAdapter.class.getMethod("getById", Long.class);
        ProceedingJoinPoint joinPoint = joinPoint(method, new DemoPersistenceAdapter(), "entity");

        Object result = sut.traceUseCaseAndAdapter(joinPoint);

        assertThat(result).isEqualTo("entity");
        then(span).should().name("adapter.persistence.DemoPersistenceAdapter.getById");
        then(span).should().tag("app.layer", "adapter.out");
        then(span).should().tag("app.domain", "demo");
        then(span).should().tag("app.adapter.type", "persistence");
        then(span).should().tag("code.function", "getById");
        then(span).should().end();
    }

    @Test
    @DisplayName("상위 클래스가 구현한 UseCase interface도 UseCase span으로 감싼다")
    void 상위_클래스_usecase_interface_탐색() throws Throwable {
        Method method = DemoUseCase.class.getMethod("getById", Long.class);
        ProceedingJoinPoint joinPoint = joinPoint(method, new InheritedDemoQueryService(), "result");

        Object result = sut.traceUseCaseAndAdapter(joinPoint);

        assertThat(result).isEqualTo("result");
        then(span).should().name("usecase.DemoUseCase.getById");
        then(span).should().tag("app.usecase", "DemoUseCase");
    }

    @Test
    @DisplayName("예외 기록이 실패해도 애플리케이션의 원본 예외를 그대로 다시 던진다")
    void 원본_예외_보존() throws Throwable {
        Method method = DemoUseCase.class.getMethod("getById", Long.class);
        // 정제 대상 메시지가 정규식 재귀를 유발해도 원본 예외가 대체되면 안 된다.
        IllegalStateException original = new IllegalStateException("'" + "''".repeat(100_000));

        ProceedingJoinPoint joinPoint = joinPoint(method, new DemoQueryService(), null);
        given(joinPoint.proceed()).willThrow(original);

        assertThatThrownBy(() -> sut.traceUseCaseAndAdapter(joinPoint)).isSameAs(original);
    }

    private ProceedingJoinPoint joinPoint(Method method, Object target, Object result) throws Throwable {
        MethodSignature signature = mock(MethodSignature.class);
        given(signature.getMethod()).willReturn(method);
        given(signature.getDeclaringType()).willReturn(method.getDeclaringClass());

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        given(joinPoint.getSignature()).willReturn(signature);
        given(joinPoint.getTarget()).willReturn(target);
        given(joinPoint.proceed()).willReturn(result);
        return joinPoint;
    }

    interface DemoUseCase {

        String getById(Long id);
    }

    static class DemoQueryService implements DemoUseCase {

        @Override
        public String getById(Long id) {
            return "result";
        }
    }

    static class BaseDemoQueryService implements DemoUseCase {

        @Override
        public String getById(Long id) {
            return "result";
        }
    }

    static class InheritedDemoQueryService extends BaseDemoQueryService {
    }

    static class DemoPersistenceAdapter {

        public String getById(Long id) {
            return "entity";
        }
    }
}
