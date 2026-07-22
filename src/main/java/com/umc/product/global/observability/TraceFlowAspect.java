package com.umc.product.global.observability;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

@Aspect
@Component
public class TraceFlowAspect {

    private static final String PRODUCT_PACKAGE_MARKER = ".product.";
    private static final String APPLICATION_SERVICE_PACKAGE = ".application.service.";
    private static final String ADAPTER_OUT_PACKAGE = ".adapter.out.";
    private static final String UNKNOWN = "unknown";

    private final Tracer tracer;
    private final ObservabilityTracingProperties properties;
    private final ConcurrentMap<TraceSpanKey, TraceSpanMetadata> metadataCache = new ConcurrentHashMap<>();

    @Autowired
    public TraceFlowAspect(ObjectProvider<Tracer> tracerProvider, ObservabilityTracingProperties properties) {
        this(tracerProvider.getIfAvailable(() -> Tracer.NOOP), properties);
    }

    TraceFlowAspect(Tracer tracer, ObservabilityTracingProperties properties) {
        this.tracer = tracer;
        this.properties = properties;
    }

    @Around("""
        (
            execution(* com.umc.product..application.service..*Service.*(..)) ||
            execution(* com.umc.product..adapter.out..*Adapter.*(..)) ||
            execution(* com.umc.product..adapter.out..*Client.*(..))
        )
        && !within(com.umc.product.global.observability..*)
        """)
    public Object traceUseCaseAndAdapter(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        TraceSpanMetadata metadata = metadata(joinPoint);
        if (!shouldTrace(metadata)) {
            return joinPoint.proceed();
        }

        Span span = tracer.nextSpan()
            .name(metadata.spanName())
            .tag("app.layer", metadata.layer())
            .tag("app.domain", metadata.domain())
            .tag("code.namespace", metadata.className())
            .tag("code.function", metadata.methodName());

        if (metadata.useCaseName() != null) {
            span.tag("app.usecase", metadata.useCaseName());
            span.tag("app.service", metadata.className());
        }
        if (metadata.adapterType() != null) {
            span.tag("app.adapter.type", metadata.adapterType());
        }

        span.start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            ObservabilityErrorSanitizer.record(span, throwable);
            throw throwable;
        } finally {
            span.end();
        }
    }

    private TraceSpanMetadata metadata(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> targetClass = TraceSpanMetadata.targetClass(joinPoint);
        Method method = TraceSpanMetadata.mostSpecificMethod(signature.getMethod(), targetClass);
        TraceSpanKey key = new TraceSpanKey(targetClass, method);
        return metadataCache.computeIfAbsent(key, ignored -> TraceSpanMetadata.from(targetClass, method));
    }

    private boolean shouldTrace(TraceSpanMetadata metadata) {
        return switch (metadata.kind()) {
            case USECASE -> properties.isUseCaseSpans();
            case ADAPTER -> properties.isAdapterSpans();
        };
    }

    private enum TraceSpanKind {
        USECASE,
        ADAPTER
    }

    private record TraceSpanKey(Class<?> targetClass, Method method) {
    }

    private record TraceSpanMetadata(
        TraceSpanKind kind,
        String spanName,
        String layer,
        String domain,
        String className,
        String methodName,
        String useCaseName,
        String adapterType
    ) {

        private static TraceSpanMetadata from(Class<?> targetClass, Method method) {
            String packageName = targetClass.getPackageName();
            String methodName = method.getName();
            String className = targetClass.getSimpleName();

            if (isApplicationService(packageName, targetClass)) {
                String useCaseName = resolveUseCaseName(targetClass, method);
                String domain = resolveDomain(packageName, className);
                String spanName = "usecase.%s.%s".formatted(useCaseName, methodName);
                return new TraceSpanMetadata(
                    TraceSpanKind.USECASE,
                    spanName,
                    "application",
                    domain,
                    className,
                    methodName,
                    useCaseName,
                    null
                );
            }

            String adapterType = resolveAdapterType(packageName, className);
            String domain = resolveDomain(packageName, className);
            String spanName = "adapter.%s.%s.%s".formatted(adapterType, className, methodName);
            return new TraceSpanMetadata(
                TraceSpanKind.ADAPTER,
                spanName,
                "adapter.out",
                domain,
                className,
                methodName,
                null,
                adapterType
            );
        }

        private static Class<?> targetClass(ProceedingJoinPoint joinPoint) {
            Object target = joinPoint.getTarget();
            if (target == null) {
                return ((MethodSignature) joinPoint.getSignature()).getDeclaringType();
            }
            return ClassUtils.getUserClass(target);
        }

        private static Method mostSpecificMethod(Method method, Class<?> targetClass) {
            return AopUtils.getMostSpecificMethod(method, targetClass);
        }

        private static boolean isApplicationService(String packageName, Class<?> targetClass) {
            return packageName.contains(APPLICATION_SERVICE_PACKAGE)
                || Arrays.stream(ClassUtils.getAllInterfacesForClass(targetClass))
                .anyMatch(type -> type.getSimpleName().endsWith("UseCase"));
        }

        private static String resolveUseCaseName(Class<?> targetClass, Method method) {
            Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass.isInterface() && declaringClass.getSimpleName().endsWith("UseCase")) {
                return declaringClass.getSimpleName();
            }

            return Arrays.stream(ClassUtils.getAllInterfacesForClass(targetClass))
                .filter(type -> type.getSimpleName().endsWith("UseCase"))
                .filter(type -> hasSameMethod(type, method))
                .min(Comparator.comparing(Class::getSimpleName))
                .map(Class::getSimpleName)
                .orElseGet(() -> classNameAsUseCase(targetClass.getSimpleName()));
        }

        private static boolean hasSameMethod(Class<?> type, Method method) {
            return Arrays.stream(type.getMethods())
                .anyMatch(candidate -> candidate.getName().equals(method.getName())
                    && Arrays.equals(candidate.getParameterTypes(), method.getParameterTypes()));
        }

        private static String classNameAsUseCase(String className) {
            return className
                .replaceFirst("CommandService$", "UseCase")
                .replaceFirst("QueryService$", "UseCase")
                .replaceFirst("Service$", "UseCase");
        }

        private static String resolveAdapterType(String packageName, String className) {
            int index = packageName.indexOf(ADAPTER_OUT_PACKAGE);
            if (index < 0) {
                if (className.endsWith("PersistenceAdapter")) {
                    return "persistence";
                }
                return "unknown";
            }

            String suffix = packageName.substring(index + ADAPTER_OUT_PACKAGE.length());
            int nextDot = suffix.indexOf('.');
            return nextDot < 0 ? suffix : suffix.substring(0, nextDot);
        }

        private static String resolveDomain(String packageName, String className) {
            int index = packageName.indexOf(PRODUCT_PACKAGE_MARKER);
            if (index >= 0) {
                String suffix = packageName.substring(index + PRODUCT_PACKAGE_MARKER.length());
                int nextDot = suffix.indexOf('.');
                String candidate = nextDot < 0 ? suffix : suffix.substring(0, nextDot);
                if (!candidate.isBlank() && !"global".equals(candidate)) {
                    return candidate;
                }
            }
            return simpleClassDomain(className);
        }

        private static String simpleClassDomain(String className) {
            String domain = className
                .replaceFirst("CommandService$", "")
                .replaceFirst("QueryService$", "")
                .replaceFirst("PersistenceAdapter$", "")
                .replaceFirst("Adapter$", "")
                .replaceFirst("Service$", "")
                .replaceFirst("UseCase$", "");
            if (domain.isBlank()) {
                return UNKNOWN;
            }
            return domain.substring(0, 1).toLowerCase(Locale.ROOT) + domain.substring(1);
        }
    }
}
