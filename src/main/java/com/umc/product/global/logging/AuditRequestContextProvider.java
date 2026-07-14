package com.umc.product.global.logging;

import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class AuditRequestContextProvider {

    private static final String MDC_TRACE_ID = "traceId";
    private static final int MAX_CORRELATION_ID_LENGTH = 100;
    private static final int MAX_IP_ADDRESS_LENGTH = 45;
    private static final Pattern CORRELATION_ID_PATTERN =
        Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]*");

    public Snapshot capture() {
        return new Snapshot(
            trustedRemoteAddress(currentRequest()),
            validCorrelationId(MDC.get(MDC_TRACE_ID)),
            currentTraceId()
        );
    }

    private String currentTraceId() {
        SpanContext spanContext = Span.current().getSpanContext();
        return spanContext.isValid() ? spanContext.getTraceId() : null;
    }

    private String validCorrelationId(String value) {
        if (value == null
            || value.isBlank()
            || value.length() > MAX_CORRELATION_ID_LENGTH
            || !CORRELATION_ID_PATTERN.matcher(value).matches()) {
            return null;
        }
        return value;
    }

    private String trustedRemoteAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        ServletRequest nativeRequest = request;
        while (nativeRequest instanceof ServletRequestWrapper wrapper) {
            nativeRequest = wrapper.getRequest();
        }
        String remoteAddress = nativeRequest.getRemoteAddr();
        if (remoteAddress == null
            || remoteAddress.isBlank()
            || remoteAddress.length() > MAX_IP_ADDRESS_LENGTH) {
            return null;
        }
        return remoteAddress;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    public record Snapshot(
        String ipAddress,
        String requestId,
        String traceId
    ) {
    }
}
