package com.umc.product.global.observability;

import java.util.regex.Pattern;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;

/**
 * 아웃박스 발행/relay 사이에서 원 요청 trace 컨텍스트를 전달하기 위한 W3C traceparent 변환 유틸.
 *
 * <p>형식: {@code 00-<trace-id(32 hex)>-<parent-id(16 hex)>-<flags(2 hex)>} (W3C Trace Context level 1).
 * 발행 시점에 현재 span 컨텍스트를 문자열로 캡처({@link #capture})해 두고, relay 시점에 복원({@link #restore})해
 * span link 의 origin TraceContext 로 사용한다.
 */
public final class W3CTraceparent {

    private static final String VERSION = "00";
    private static final String FLAG_SAMPLED = "01";
    private static final String FLAG_NOT_SAMPLED = "00";
    private static final Pattern TRACEPARENT_PATTERN =
        Pattern.compile("00-([0-9a-f]{32})-([0-9a-f]{16})-([0-9a-f]{2})");

    private W3CTraceparent() {
    }

    /**
     * 현재 활성 span 의 컨텍스트를 W3C traceparent 문자열로 캡처한다.
     *
     * @return 활성 span 이 없으면 {@code null}
     */
    public static String capture(Tracer tracer) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return null;
        }
        TraceContext context = currentSpan.context();
        String flags = Boolean.TRUE.equals(context.sampled()) ? FLAG_SAMPLED : FLAG_NOT_SAMPLED;
        return String.join("-", VERSION, context.traceId(), context.spanId(), flags);
    }

    /**
     * 저장된 traceparent 문자열을 {@link TraceContext} 로 복원한다.
     *
     * @return 값이 비었거나 형식이 올바르지 않으면 {@code null}
     */
    public static TraceContext restore(Tracer tracer, String traceparent) {
        if (traceparent == null || traceparent.isBlank()) {
            return null;
        }
        var matcher = TRACEPARENT_PATTERN.matcher(traceparent);
        if (!matcher.matches()) {
            return null;
        }
        boolean sampled = FLAG_SAMPLED.equals(matcher.group(3));
        return tracer.traceContextBuilder()
            .traceId(matcher.group(1))
            .spanId(matcher.group(2))
            .sampled(sampled)
            .build();
    }
}
