package com.umc.product.global.logging;

import java.util.List;
import java.util.Map;

import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import com.umc.product.global.observability.ObservabilityErrorSanitizer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;

/**
 * 민감정보가 제거된 값을 돌려주는 {@link ILoggingEvent} 읽기 전용 데코레이터다.
 *
 * <p>이벤트를 새로 만들지 않고 원본을 감싸기만 한다. 덕분에 caller data, 타임스탬프, 시퀀스 번호,
 * 마커가 원형을 유지하며, 이전 구현이 {@code new LoggingEvent(...)}로 재발행하면서 호출 위치 정보를
 * 필터 클래스로 오염시키던 문제가 발생하지 않는다.
 *
 * <p>정제 결과는 생성 시점에 한 번만 계산한다. 정제 어펜더 아래에 여러 출력 어펜더가 붙어도 비용은
 * 한 번이고, 필드가 불변이므로 스레드 간 가시성 문제도 없다.
 */
final class SanitizedLoggingEvent implements ILoggingEvent {

    private final ILoggingEvent delegate;
    private final String message;
    private final String formattedMessage;
    private final IThrowableProxy throwableProxy;

    private SanitizedLoggingEvent(ILoggingEvent delegate) {
        this.delegate = delegate;
        this.message = ObservabilityErrorSanitizer.sanitizeMessage(delegate.getMessage());
        this.formattedMessage = ObservabilityErrorSanitizer.sanitizeMessage(delegate.getFormattedMessage());
        this.throwableProxy = SanitizedThrowableProxy.wrap(delegate.getThrowableProxy());
    }

    static ILoggingEvent wrap(ILoggingEvent event) {
        return new SanitizedLoggingEvent(event);
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getFormattedMessage() {
        return formattedMessage;
    }

    @Override
    public IThrowableProxy getThrowableProxy() {
        return throwableProxy;
    }

    /**
     * 파라미터 배열은 그대로 위임한다. 사람이 읽는 값은 이미 {@link #getFormattedMessage()}에서 정제됐고,
     * 이 배열을 따로 소비하는 것은 구조화 인자 provider 뿐이다. 여기서 {@code String.valueOf(...)}로
     * 전부 렌더링하면 slf4j 지연 포매팅이 무력화되고 큰 객체가 통째로 문자열이 된다.
     */
    @Override
    public Object[] getArgumentArray() {
        return delegate.getArgumentArray();
    }

    @Override
    public String getThreadName() {
        return delegate.getThreadName();
    }

    @Override
    public Level getLevel() {
        return delegate.getLevel();
    }

    @Override
    public String getLoggerName() {
        return delegate.getLoggerName();
    }

    @Override
    public LoggerContextVO getLoggerContextVO() {
        return delegate.getLoggerContextVO();
    }

    @Override
    public StackTraceElement[] getCallerData() {
        return delegate.getCallerData();
    }

    @Override
    public boolean hasCallerData() {
        return delegate.hasCallerData();
    }

    @Override
    @SuppressWarnings("deprecation")
    public Marker getMarker() {
        return delegate.getMarker();
    }

    @Override
    public List<Marker> getMarkerList() {
        return delegate.getMarkerList();
    }

    @Override
    public Map<String, String> getMDCPropertyMap() {
        return delegate.getMDCPropertyMap();
    }

    @Override
    @SuppressWarnings("deprecation")
    public Map<String, String> getMdc() {
        return delegate.getMdc();
    }

    @Override
    public long getTimeStamp() {
        return delegate.getTimeStamp();
    }

    @Override
    public int getNanoseconds() {
        return delegate.getNanoseconds();
    }

    @Override
    public long getSequenceNumber() {
        return delegate.getSequenceNumber();
    }

    @Override
    public List<KeyValuePair> getKeyValuePairs() {
        return delegate.getKeyValuePairs();
    }

    @Override
    public void prepareForDeferredProcessing() {
        delegate.prepareForDeferredProcessing();
    }
}
