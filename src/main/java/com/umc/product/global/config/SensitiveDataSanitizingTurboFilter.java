package com.umc.product.global.config;

import java.util.Objects;

import org.slf4j.Marker;

import com.umc.product.global.observability.ObservabilityErrorSanitizer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;

public class SensitiveDataSanitizingTurboFilter extends TurboFilter {

    @Override
    public FilterReply decide(
        Marker marker,
        Logger logger,
        Level level,
        String format,
        Object[] parameters,
        Throwable throwable
    ) {
        SanitizedArguments sanitized = sanitize(format, parameters, throwable);
        if (!sanitized.changed()) {
            return FilterReply.NEUTRAL;
        }

        LoggingEvent event = new LoggingEvent(
            SensitiveDataSanitizingTurboFilter.class.getName(),
            logger,
            level,
            sanitized.format(),
            sanitized.throwable(),
            sanitized.parameters()
        );
        if (marker != null) {
            event.addMarker(marker);
        }
        logger.callAppenders(event);
        return FilterReply.DENY;
    }

    private SanitizedArguments sanitize(String format, Object[] parameters, Throwable throwable) {
        String sanitizedFormat = ObservabilityErrorSanitizer.sanitizeMessage(format);
        boolean changed = !Objects.equals(format, sanitizedFormat);

        Object[] sanitizedParameters = parameters;
        if (parameters != null) {
            sanitizedParameters = parameters.clone();
            for (int index = 0; index < parameters.length; index++) {
                Object parameter = parameters[index];
                Object sanitizedParameter = sanitizeParameter(parameter);
                sanitizedParameters[index] = sanitizedParameter;
                changed |= sanitizedParameter != parameter;
            }
        }

        Throwable sanitizedThrowable = ObservabilityErrorSanitizer.sanitize(throwable);
        changed |= sanitizedThrowable != throwable;
        if (sanitizedThrowable == null && sanitizedParameters != null && sanitizedParameters.length > 0) {
            Object lastParameter = sanitizedParameters[sanitizedParameters.length - 1];
            if (lastParameter instanceof Throwable parameterThrowable) {
                sanitizedThrowable = parameterThrowable;
            }
        }
        return new SanitizedArguments(sanitizedFormat, sanitizedParameters, sanitizedThrowable, changed);
    }

    private Object sanitizeParameter(Object parameter) {
        if (parameter instanceof Throwable error) {
            return ObservabilityErrorSanitizer.sanitize(error);
        }
        if (parameter == null) {
            return null;
        }

        String rendered = String.valueOf(parameter);
        String sanitized = ObservabilityErrorSanitizer.sanitizeMessage(rendered);
        return rendered.equals(sanitized) ? parameter : sanitized;
    }

    private record SanitizedArguments(
        String format,
        Object[] parameters,
        Throwable throwable,
        boolean changed
    ) {
    }
}
