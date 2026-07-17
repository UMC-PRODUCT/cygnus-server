package com.umc.product.global.observability;

import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.micrometer.tracing.Span;

public final class ObservabilityErrorSanitizer {

    public static final String REDACTED = "[REDACTED]";

    private static final Pattern POSTGRES_KEY_DETAIL = Pattern.compile(
        "(?is)(\\bDetail:\\s*Key\\s*\\([^\\r\\n)]*\\)\\s*=\\s*)\\([^\\r\\n)]*\\)"
    );
    private static final Pattern BIND_DETAIL = Pattern.compile(
        "(?i)(\\bbinding parameter \\[\\d+] as \\[[^]\\r\\n]+]\\s*-\\s*)\\[[^]\\r\\n]*]"
    );
    private static final Pattern APPLICATION_KEY_VALUE = Pattern.compile(
        "(?i)(\\bapplication[_ ]?key\\b\\s*[=:]\\s*)[A-Z0-9]{6}"
    );
    private static final Pattern EMAIL = Pattern.compile(
        "(?i)(?<![A-Z0-9._%+-])[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}(?![A-Z0-9._%+-])"
    );
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)(\\bBearer\\s+)[A-Za-z0-9._~+/-]+=*");
    private static final Pattern JWT = Pattern.compile(
        "\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b"
    );
    private static final Pattern SINGLE_QUOTED_VALUE = Pattern.compile("'(?:''|[^'])*'", Pattern.DOTALL);
    private static final Pattern DOUBLE_QUOTED_VALUE = Pattern.compile("\"(?:\"\"|[^\"])*\"", Pattern.DOTALL);
    private static final Pattern CONSTRAINT_PREFIX = Pattern.compile("(?i)\\bconstraint\\s*$");
    private static final Pattern MIXED_APPLICATION_KEY = Pattern.compile(
        "\\b(?=[A-Z0-9]{6}\\b)(?=[A-Z0-9]*[A-Z])(?=[A-Z0-9]*\\d)[A-Z0-9]{6}\\b"
    );
    private static final Pattern CONSTRAINT_NAME = Pattern.compile(
        "(?i)\\bconstraint\\s+\"([A-Za-z0-9_.-]+)\""
    );

    private ObservabilityErrorSanitizer() {
    }

    public static void record(Span span, Throwable error) {
        ErrorMetadata metadata = ErrorMetadata.from(error);
        span.tag("app.error.class", metadata.errorClass());
        if (metadata.sqlErrorClass() != null) {
            span.tag("db.error.class", metadata.sqlErrorClass());
            tagIfPresent(span, "db.response.sql_state", metadata.sqlState());
            span.tag("db.response.vendor_code", String.valueOf(metadata.vendorCode()));
            tagIfPresent(span, "db.constraint.name", metadata.constraintName());
        }
        span.error(sanitize(error));
    }

    public static Throwable sanitize(Throwable error) {
        if (error == null || !containsSensitiveMessage(error, newIdentitySet())) {
            return error;
        }
        return copy(error, new IdentityHashMap<>());
    }

    public static String sanitizeMessage(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String sanitized = POSTGRES_KEY_DETAIL.matcher(message).replaceAll("$1(" + REDACTED + ")");
        sanitized = BIND_DETAIL.matcher(sanitized).replaceAll("$1[" + REDACTED + "]");
        sanitized = APPLICATION_KEY_VALUE.matcher(sanitized).replaceAll("$1" + REDACTED);
        sanitized = EMAIL.matcher(sanitized).replaceAll(REDACTED);
        sanitized = BEARER_TOKEN.matcher(sanitized).replaceAll("$1" + REDACTED);
        sanitized = JWT.matcher(sanitized).replaceAll(REDACTED);
        sanitized = SINGLE_QUOTED_VALUE.matcher(sanitized).replaceAll("'" + REDACTED + "'");
        sanitized = redactDoubleQuotedValues(sanitized);
        return MIXED_APPLICATION_KEY.matcher(sanitized).replaceAll(REDACTED);
    }

    private static String redactDoubleQuotedValues(String message) {
        Matcher matcher = DOUBLE_QUOTED_VALUE.matcher(message);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String prefix = message.substring(Math.max(0, matcher.start() - 32), matcher.start());
            String replacement = CONSTRAINT_PREFIX.matcher(prefix).find() ? matcher.group() : "\"" + REDACTED + "\"";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static boolean containsSensitiveMessage(Throwable error, Set<Throwable> visited) {
        if (error == null || !visited.add(error)) {
            return false;
        }
        String message = error.getMessage();
        if (message != null && !message.equals(sanitizeMessage(message))) {
            return true;
        }
        if (containsSensitiveMessage(error.getCause(), visited)) {
            return true;
        }
        for (Throwable suppressed : error.getSuppressed()) {
            if (containsSensitiveMessage(suppressed, visited)) {
                return true;
            }
        }
        return false;
    }

    private static Throwable copy(Throwable source, IdentityHashMap<Throwable, Throwable> copies) {
        Throwable existing = copies.get(source);
        if (existing != null) {
            return existing;
        }

        ErrorMetadata metadata = ErrorMetadata.from(source);
        SanitizedObservabilityException copy = new SanitizedObservabilityException(metadata.summary(source));
        copy.setStackTrace(source.getStackTrace());
        copies.put(source, copy);

        if (source.getCause() != null) {
            copy.initCause(copy(source.getCause(), copies));
        }
        for (Throwable suppressed : source.getSuppressed()) {
            copy.addSuppressed(copy(suppressed, copies));
        }
        return copy;
    }

    private static Set<Throwable> newIdentitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private static void tagIfPresent(Span span, String key, String value) {
        if (value != null && !value.isBlank()) {
            span.tag(key, value);
        }
    }

    public record ErrorMetadata(
        String errorClass,
        String sqlErrorClass,
        String sqlState,
        int vendorCode,
        String constraintName
    ) {

        private static ErrorMetadata from(Throwable error) {
            String errorClass = error == null ? "unknown" : error.getClass().getName();
            SQLException sqlException = findSqlException(error, newIdentitySet());
            String constraintName = findConstraintName(error, newIdentitySet());
            if (sqlException == null) {
                return new ErrorMetadata(errorClass, null, null, 0, constraintName);
            }
            return new ErrorMetadata(
                errorClass,
                sqlException.getClass().getName(),
                sqlException.getSQLState(),
                sqlException.getErrorCode(),
                constraintName
            );
        }

        private String summary(Throwable error) {
            StringBuilder summary = new StringBuilder("errorClass=").append(errorClass);
            if (sqlErrorClass != null) {
                summary.append(", sqlErrorClass=").append(sqlErrorClass);
            }
            if (sqlState != null) {
                summary.append(", sqlState=").append(sqlState);
            }
            summary.append(", vendorCode=").append(vendorCode);
            if (constraintName != null) {
                summary.append(", constraint=").append(constraintName);
            }
            summary.append(", message=").append(sanitizeMessage(error.getMessage()));
            return summary.toString();
        }

        private static SQLException findSqlException(Throwable error, Set<Throwable> visited) {
            if (error == null || !visited.add(error)) {
                return null;
            }
            if (error instanceof SQLException sqlException) {
                return sqlException;
            }
            SQLException cause = findSqlException(error.getCause(), visited);
            if (cause != null) {
                return cause;
            }
            for (Throwable suppressed : error.getSuppressed()) {
                SQLException found = findSqlException(suppressed, visited);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }

        private static String findConstraintName(Throwable error, Set<Throwable> visited) {
            if (error == null || !visited.add(error)) {
                return null;
            }
            Matcher matcher = CONSTRAINT_NAME.matcher(String.valueOf(error.getMessage()));
            if (matcher.find()) {
                return matcher.group(1);
            }
            String cause = findConstraintName(error.getCause(), visited);
            if (cause != null) {
                return cause;
            }
            for (Throwable suppressed : error.getSuppressed()) {
                String found = findConstraintName(suppressed, visited);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
    }

    private static final class SanitizedObservabilityException extends RuntimeException {

        private SanitizedObservabilityException(String message) {
            super(message);
        }
    }
}
