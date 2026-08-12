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
    private static final Pattern FORM_RESPONSE_ACCESS_KEY_VALUE = Pattern.compile(
        "(?i)(\\b(?:responseAccessKey|formResponseAccessKey|response_access_key|form_response_access_key)"
            + "\\b\\s*[=:]\\s*)[^\\s,}\\]]+"
    );
    private static final Pattern EMAIL = Pattern.compile(
        "(?i)(?<![A-Z0-9._%+-])[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}(?![A-Z0-9._%+-])"
    );
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)(\\bBearer\\s+)[A-Za-z0-9._~+/-]+=*");
    private static final Pattern JWT = Pattern.compile(
        "\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b"
    );
    // 그룹 반복은 문자마다 재귀하므로 긴 메시지에서 StackOverflowError가 난다.
    private static final Pattern SINGLE_QUOTED_VALUE = Pattern.compile("'[^']*(?:''[^']*)*'", Pattern.DOTALL);
    private static final Pattern DOUBLE_QUOTED_VALUE = Pattern.compile("\"[^\"]*(?:\"\"[^\"]*)*\"", Pattern.DOTALL);
    private static final Pattern CONSTRAINT_PREFIX = Pattern.compile("(?i)\\bconstraint\\s*$");
    private static final Pattern MIXED_APPLICATION_KEY = Pattern.compile(
        "\\b(?=[A-Z0-9]{6}\\b)(?=[A-Z0-9]*[A-Z])(?=[A-Z0-9]*\\d)[A-Z0-9]{6}\\b"
    );
    private static final Pattern CONSTRAINT_NAME = Pattern.compile(
        "(?i)\\bconstraint\\s+\"([A-Za-z0-9_.-]+)\""
    );

    private ObservabilityErrorSanitizer() {
    }

    /**
     * 새니타이저 호출자는 대부분 catch 블록이거나 로깅 경로다. 이곳에서 던진 예외는 원본 예외를
     * 대체하거나 무관한 비즈니스 로직을 중단시키므로, 공개 진입점은 어떤 실패도 밖으로 내보내지 않는다.
     *
     * <p>{@code StackOverflowError}만 예외적으로 잡는다. 예외 전파 과정에서 스택이 풀리므로 catch
     * 시점에는 복구 가능한 상태다. {@code OutOfMemoryError} 등 나머지 {@code Error}는 심각한 상태를
     * 은폐하지 않도록 그대로 전파한다.
     */
    public static void record(Span span, Throwable error) {
        try {
            ErrorMetadata metadata = ErrorMetadata.from(error);
            span.tag("app.error.class", metadata.errorClass());
            if (metadata.sqlErrorClass() != null) {
                span.tag("db.error.class", metadata.sqlErrorClass());
                tagIfPresent(span, "db.response.sql_state", metadata.sqlState());
                span.tag("db.response.vendor_code", String.valueOf(metadata.vendorCode()));
                tagIfPresent(span, "db.constraint.name", metadata.constraintName());
            }
            span.error(sanitize(error));
        } catch (RuntimeException | StackOverflowError failure) {
            span.tag("app.error.sanitize_failed", failure.getClass().getSimpleName());
        }
    }

    public static Throwable sanitize(Throwable error) {
        if (error == null) {
            return null;
        }
        try {
            if (!containsSensitiveMessage(error, newIdentitySet())) {
                return error;
            }
            return copy(error, new IdentityHashMap<>());
        } catch (RuntimeException | StackOverflowError failure) {
            // 정제 여부를 판단하지 못했으므로 원본을 그대로 내보내지 않는다.
            return new SanitizedObservabilityException(
                "errorClass=" + error.getClass().getName() + ", message=" + failureMarker(failure, null)
            );
        }
    }

    public static String sanitizeMessage(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        try {
            return redact(message);
        } catch (RuntimeException | StackOverflowError failure) {
            return failureMarker(failure, message);
        }
    }

    /**
     * 정제에 실패했을 때 남기는 마커. 정제가 완주하지 못했다는 것은 남은 내용을 확인할 수 없다는
     * 뜻이므로 원문을 내보내지 않는다({@code fail-closed}). 대신 {@link #REDACTED}와 구분되는
     * 마커와 안전한 메타데이터만 남겨 새니타이저 고장을 민감값 치환과 혼동하지 않게 한다.
     *
     * <p>여기서 로깅하지 않는다. 정제 어펜더가 재진입해 실패 상황에서 재귀할 수 있다.
     */
    private static String failureMarker(Throwable failure, String message) {
        String length = message == null ? "unknown" : String.valueOf(message.length());
        return "[SANITIZE_FAILED: " + failure.getClass().getSimpleName() + ", length=" + length + "]";
    }

    private static String redact(String message) {
        String sanitized = POSTGRES_KEY_DETAIL.matcher(message).replaceAll("$1(" + REDACTED + ")");
        sanitized = BIND_DETAIL.matcher(sanitized).replaceAll("$1[" + REDACTED + "]");
        sanitized = APPLICATION_KEY_VALUE.matcher(sanitized).replaceAll("$1" + REDACTED);
        sanitized = FORM_RESPONSE_ACCESS_KEY_VALUE.matcher(sanitized).replaceAll("$1" + REDACTED);
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
