package com.umc.product.global.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
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

    /**
     * Logback 은 {@code ThrowableProxy}를 만들 때 순환을 이미 끊어 마커 노드로 대체하므로
     * ({@code isCyclic()}은 순회 중단 신호가 아니라 렌더링 힌트다) 같은 인스턴스가 두 번 나오지 않는다.
     * {@code visited}는 그 보장이 깨졌을 때의 그물이며, 재방문하면 원본을 흘리는 대신 체인을 끊는다.
     */
    private static IThrowableProxy wrap(IThrowableProxy proxy, Set<IThrowableProxy> visited) {
        if (proxy == null || !visited.add(proxy)) {
            return null;
        }

        String message = proxy.getOverridingMessage() != null ? proxy.getOverridingMessage() : proxy.getMessage();
        String sanitizedMessage = ObservabilityErrorSanitizer.sanitizeMessage(message);
        IThrowableProxy sanitizedCause = wrap(proxy.getCause(), visited);
        IThrowableProxy[] sanitizedSuppressed = wrapSuppressed(proxy.getSuppressed(), visited);

        return new SanitizedThrowableProxy(proxy, sanitizedMessage, sanitizedCause, sanitizedSuppressed);
    }

    private static IThrowableProxy[] wrapSuppressed(IThrowableProxy[] suppressed, Set<IThrowableProxy> visited) {
        if (suppressed == null || suppressed.length == 0) {
            return suppressed;
        }

        // 체인이 끊긴 자리는 null 이 되므로, 소비자가 순회 중 NPE 를 만나지 않도록 제외한다.
        List<IThrowableProxy> wrapped = new ArrayList<>(suppressed.length);
        for (IThrowableProxy each : suppressed) {
            IThrowableProxy sanitized = wrap(each, visited);
            if (sanitized != null) {
                wrapped.add(sanitized);
            }
        }
        return wrapped.toArray(new IThrowableProxy[0]);
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
