package com.umc.product.global.logging;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import com.umc.product.global.observability.ObservabilityErrorSanitizer;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;

/**
 * 예외 메시지만 정제하고 나머지는 원본에 위임하는 {@link IThrowableProxy} 데코레이터다.
 *
 * <p>어펜더 단계에서 예외는 이미 {@code Throwable}이 아니라 {@link IThrowableProxy}로 변환되어 있다.
 * Logback은 이 상황을 위해 {@link IThrowableProxy#getOverridingMessage()} 훅을 제공하고,
 * {@code ThrowableProxyUtil.appendNominalOrOverridingMessage()}가 non-null이면 원래 메시지 대신
 * 그 값을 출력한다. 덕분에 스택트레이스·클래스명·프레임을 원형 그대로 두고 메시지만 교체할 수 있다.
 *
 * <p>{@code getMessage()}도 함께 정제한다. 훅을 참조하지 않고 메시지를 직접 읽는 소비자
 * (OTLP 어펜더 등)가 있기 때문이다.
 */
final class SanitizedThrowableProxy implements IThrowableProxy {

    private final IThrowableProxy delegate;
    private final String sanitizedMessage;
    private final IThrowableProxy cause;
    private final IThrowableProxy[] suppressed;

    private SanitizedThrowableProxy(
        IThrowableProxy delegate,
        String sanitizedMessage,
        IThrowableProxy cause,
        IThrowableProxy[] suppressed
    ) {
        this.delegate = delegate;
        this.sanitizedMessage = sanitizedMessage;
        this.cause = cause;
        this.suppressed = suppressed;
    }

    static IThrowableProxy wrap(IThrowableProxy proxy) {
        return wrap(proxy, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static IThrowableProxy wrap(IThrowableProxy proxy, Set<IThrowableProxy> visited) {
        if (proxy == null || !visited.add(proxy)) {
            return proxy;
        }

        String message = proxy.getOverridingMessage() != null ? proxy.getOverridingMessage() : proxy.getMessage();
        String sanitizedMessage = ObservabilityErrorSanitizer.sanitizeMessage(message);
        IThrowableProxy sanitizedCause = proxy.isCyclic() ? proxy.getCause() : wrap(proxy.getCause(), visited);
        IThrowableProxy[] sanitizedSuppressed = wrapSuppressed(proxy.getSuppressed(), visited);

        return new SanitizedThrowableProxy(proxy, sanitizedMessage, sanitizedCause, sanitizedSuppressed);
    }

    private static IThrowableProxy[] wrapSuppressed(IThrowableProxy[] suppressed, Set<IThrowableProxy> visited) {
        if (suppressed == null || suppressed.length == 0) {
            return suppressed;
        }

        IThrowableProxy[] wrapped = new IThrowableProxy[suppressed.length];
        for (int index = 0; index < suppressed.length; index++) {
            wrapped[index] = wrap(suppressed[index], visited);
        }
        return wrapped;
    }

    @Override
    public String getOverridingMessage() {
        return sanitizedMessage;
    }

    @Override
    public String getMessage() {
        return sanitizedMessage;
    }

    @Override
    public String getClassName() {
        return delegate.getClassName();
    }

    @Override
    public StackTraceElementProxy[] getStackTraceElementProxyArray() {
        return delegate.getStackTraceElementProxyArray();
    }

    @Override
    public int getCommonFrames() {
        return delegate.getCommonFrames();
    }

    @Override
    public IThrowableProxy getCause() {
        return cause;
    }

    @Override
    public IThrowableProxy[] getSuppressed() {
        return suppressed;
    }

    @Override
    public boolean isCyclic() {
        return delegate.isCyclic();
    }
}
