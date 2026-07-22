package com.umc.product.global.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

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
    @DisplayName("동일한 target class와 method의 trace metadata를 캐시한다")
    void 동일_메서드_trace_metadata_캐시() throws Throwable {
        Method method = DemoUseCase.class.getMethod("getById", Long.class);
        DemoQueryService target = new DemoQueryService();

        sut.traceUseCaseAndAdapter(joinPoint(method, target, "first"));
        sut.traceUseCaseAndAdapter(joinPoint(method, target, "second"));

        Field cacheField = TraceFlowAspect.class.getDeclaredField("metadataCache");
        cacheField.setAccessible(true);
        Map<?, ?> metadataCache = (Map<?, ?>) cacheField.get(sut);

        assertThat(metadataCache).hasSize(1);
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
    @DisplayName("비활성 usecase span은 span을 만들지 않고 원 호출만 진행한다")
    void usecase_span_disabled() throws Throwable {
        ObservabilityTracingProperties properties = new ObservabilityTracingProperties();
        properties.setUseCaseSpans(false);
        sut = new TraceFlowAspect(tracer, properties);
        Method method = DemoUseCase.class.getMethod("getById", Long.class);

        Object result = sut.traceUseCaseAndAdapter(joinPoint(method, new DemoQueryService(), "result"));

        assertThat(result).isEqualTo("result");
        then(tracer).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("trace 대상 호출 실패는 정제된 error를 기록하고 원 예외를 다시 던진다")
    void traced_invocation_failure() throws Throwable {
        IllegalStateException failure = new IllegalStateException("person@example.invalid");
        Method method = DemoUseCase.class.getMethod("getById", Long.class);
        ProceedingJoinPoint joinPoint = joinPoint(method, new DemoQueryService(), null);
        given(joinPoint.proceed()).willThrow(failure);

        org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> sut.traceUseCaseAndAdapter(joinPoint)
        ).isSameAs(failure);

        then(span).should().error(org.mockito.ArgumentMatchers.argThat(error ->
            error != failure && !error.getMessage().contains("person@example.invalid")
        ));
        then(span).should().end();
    }

    @Test
    @DisplayName("target이 없는 join point는 signature 선언 type으로 metadata를 계산한다")
    void null_target_uses_declaring_type() throws Throwable {
        Method method = DemoUseCase.class.getMethod("getById", Long.class);
        ProceedingJoinPoint joinPoint = joinPoint(method, null, "result");

        assertThat(sut.traceUseCaseAndAdapter(joinPoint)).isEqualTo("result");

        then(span).should().name("usecase.DemoUseCase.getById");
    }

    @Test
    @DisplayName("provider 생성자는 Tracer bean이 없어도 NOOP tracer를 사용한다")
    void constructor_without_tracer_bean() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        ObjectProvider<Tracer> provider = beanFactory.getBeanProvider(Tracer.class);

        assertThat(new TraceFlowAspect(provider, new ObservabilityTracingProperties())).isNotNull();
    }

    @Test
    @DisplayName("metadata naming은 service suffix와 adapter package·domain 경계를 안정적으로 해석한다")
    void metadata_naming_edges() throws Exception {
        Class<?> metadataType = Class.forName(
            TraceFlowAspect.class.getName() + "$TraceSpanMetadata"
        );

        assertThat(invoke(metadataType, "classNameAsUseCase", "DemoCommandService"))
            .isEqualTo("DemoUseCase");
        assertThat(invoke(metadataType, "classNameAsUseCase", "DemoQueryService"))
            .isEqualTo("DemoUseCase");
        assertThat(invoke(metadataType, "classNameAsUseCase", "DemoService"))
            .isEqualTo("DemoUseCase");
        assertThat(invoke(metadataType, "resolveAdapterType", "com.example", "DemoPersistenceAdapter"))
            .isEqualTo("persistence");
        assertThat(invoke(metadataType, "resolveAdapterType", "com.example", "DemoClient"))
            .isEqualTo("unknown");
        assertThat(invoke(
            metadataType,
            "resolveAdapterType",
            "com.umc.product.demo.adapter.out.http.nested",
            "DemoClient"
        )).isEqualTo("http");
        assertThat(invoke(
            metadataType,
            "resolveAdapterType",
            "com.umc.product.demo.adapter.out.http",
            "DemoClient"
        )).isEqualTo("http");
        assertThat(invoke(
            metadataType,
            "resolveDomain",
            "com.umc.product.demo.adapter.out.http",
            "DemoClient"
        )).isEqualTo("demo");
        assertThat(invoke(metadataType, "resolveDomain", "com.example", "Service"))
            .isEqualTo("unknown");
    }

    private Object invoke(Class<?> type, String method, Object... arguments) {
        return ReflectionTestUtils.invokeMethod(type, method, arguments);
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
