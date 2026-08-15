package com.umc.product.global.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import net.logstash.logback.marker.ObjectAppendingMarker;
import net.logstash.logback.marker.SingleFieldAppendingMarker;

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
    private final Object[] argumentArray;
    private final Map<String, String> mdcPropertyMap;
    private final List<KeyValuePair> keyValuePairs;

    private SanitizedLoggingEvent(ILoggingEvent delegate) {
        this.delegate = delegate;
        this.message = safely(() -> ObservabilityErrorSanitizer.sanitizeMessage(delegate.getMessage()), null);
        this.formattedMessage =
            safely(() -> ObservabilityErrorSanitizer.sanitizeMessage(delegate.getFormattedMessage()), null);
        this.throwableProxy = safely(() -> SanitizedThrowableProxy.wrap(delegate.getThrowableProxy()), null);
        this.argumentArray = safely(() -> sanitizeArguments(delegate.getArgumentArray()), null);
        this.mdcPropertyMap = safely(() -> sanitizeValues(delegate.getMDCPropertyMap()), Map.of());
        this.keyValuePairs = safely(() -> sanitizeKeyValuePairs(delegate.getKeyValuePairs()), List.of());
    }

    static ILoggingEvent wrap(ILoggingEvent event) {
        return new SanitizedLoggingEvent(event);
    }

    /**
     * 접근자 하나가 실패해도 이벤트 전체를 잃지 않는다.
     *
     * <p>{@code AppenderBase.doAppend()}는 어펜더가 던진 예외를 삼키고 상태 메시지만 남긴다. 즉 여기서
     * 예외가 새어 나가면 해당 로그 한 줄이 <b>조용히 사라진다</b>. 정제 실패로 로그를 잃는 것은
     * 새니타이저가 막으려는 문제보다 나쁘므로, 실패한 필드만 안전한 기본값으로 낮춘다.
     *
     * <p>기본값은 fail-closed 를 따른다. 값을 읽지 못했다는 것은 그 안에 무엇이 있는지 확인할 수
     * 없다는 뜻이므로, 원본으로 되돌리지 않고 비운다.
     */
    private static <T> T safely(Supplier<T> sanitization, T fallback) {
        try {
            return sanitization.get();
        } catch (RuntimeException | StackOverflowError failure) {
            return fallback;
        }
    }

    /**
     * 구조화 인자만 정제한다. 사람이 읽는 값은 이미 {@link #getFormattedMessage()}에서 처리됐고,
     * 파라미터 배열을 따로 소비하는 것은 구조화 인자 provider 뿐이다. 일반 파라미터까지
     * {@code String.valueOf(...)}로 렌더링하면 slf4j 지연 포매팅이 무력화되고 큰 객체가 통째로
     * 문자열이 된다 — 이전 구현이 그렇게 동작하다 장애를 만들었다.
     *
     * <p>{@code SingleFieldAppendingMarker}는 필드명만 공개하고 값은 {@code protected}라 읽을 수 없다.
     * 그래서 값을 부분 치환하지 않고 필드 전체를 {@link ObservabilityErrorSanitizer#REDACTED}로 교체한다.
     * 필드 자체는 JSON에 남으므로, 인자를 평문 문자열로 평탄화해 필드가 사라지던 이전 동작보다 낫다.
     */
    private static Object[] sanitizeArguments(Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return arguments;
        }

        Object[] sanitized = null;
        for (int index = 0; index < arguments.length; index++) {
            if (!(arguments[index] instanceof SingleFieldAppendingMarker marker)) {
                continue;
            }
            String rendered = marker.toStringSelf();
            if (rendered.equals(ObservabilityErrorSanitizer.sanitizeMessage(rendered))) {
                continue;
            }
            if (sanitized == null) {
                sanitized = arguments.clone();
            }
            sanitized[index] = new ObjectAppendingMarker(marker.getFieldName(), ObservabilityErrorSanitizer.REDACTED);
        }
        return sanitized == null ? arguments : sanitized;
    }

    /**
     * MDC 값은 JSON 인코더의 {@code <mdc/>} provider 와 OTLP 어펜더의 {@code captureMdcAttributes}
     * 로 그대로 나간다. 이전 구현({@code TurboFilter})은 MDC 에 접근하지 못해 사각지대였다.
     */
    private static Map<String, String> sanitizeValues(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return source;
        }

        Map<String, String> sanitized = null;
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String value = entry.getValue();
            String redacted = ObservabilityErrorSanitizer.sanitizeMessage(value);
            if (Objects.equals(value, redacted)) {
                continue;
            }
            if (sanitized == null) {
                sanitized = new LinkedHashMap<>(source);
            }
            sanitized.put(entry.getKey(), redacted);
        }
        return sanitized == null ? source : Collections.unmodifiableMap(sanitized);
    }

    /**
     * {@code CharSequence} 값만 정제한다. 숫자·불리언·enum 은 민감정보를 담을 수 없고,
     * 임의 객체를 렌더링하면 파라미터 강제 렌더링과 같은 문제가 생긴다.
     */
    private static List<KeyValuePair> sanitizeKeyValuePairs(List<KeyValuePair> source) {
        if (source == null || source.isEmpty()) {
            return source;
        }

        List<KeyValuePair> sanitized = null;
        for (int index = 0; index < source.size(); index++) {
            KeyValuePair pair = source.get(index);
            if (!(pair.value instanceof CharSequence text)) {
                continue;
            }
            String redacted = ObservabilityErrorSanitizer.sanitizeMessage(text.toString());
            if (redacted.contentEquals(text)) {
                continue;
            }
            if (sanitized == null) {
                sanitized = new ArrayList<>(source);
            }
            sanitized.set(index, new KeyValuePair(pair.key, redacted));
        }
        return sanitized == null ? source : Collections.unmodifiableList(sanitized);
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

    @Override
    public Object[] getArgumentArray() {
        return argumentArray;
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
        return mdcPropertyMap;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Map<String, String> getMdc() {
        return mdcPropertyMap;
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
        return keyValuePairs;
    }

    @Override
    public void prepareForDeferredProcessing() {
        delegate.prepareForDeferredProcessing();
    }
}
