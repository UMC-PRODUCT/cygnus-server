package com.umc.product.global.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ObservabilityErrorSanitizerTest {

    @Test
    @DisplayName("quoted value와 이메일, 지원 키, bind detail, token을 일관된 marker로 치환한다")
    void 민감_메시지_redaction() {
        String raw = """
            value='secret', quoted="private", email=person@example.invalid, application_key=A1B2C3
            responseAccessKey=raw-form-secret, formResponseAccessKey=other-raw-secret
            binding parameter [2] as [VARCHAR] - [bound-secret]
            Authorization: Bearer opaque.secret-token
            """;

        String sanitized = ObservabilityErrorSanitizer.sanitizeMessage(raw);

        assertThat(sanitized)
            .contains("'[REDACTED]'", "\"[REDACTED]\"", "application_key=[REDACTED]", "Bearer [REDACTED]")
            .contains("responseAccessKey=[REDACTED]", "formResponseAccessKey=[REDACTED]")
            .doesNotContain(
                "secret",
                "private",
                "person@example.invalid",
                "A1B2C3",
                "bound-secret",
                "raw-form-secret",
                "other-raw-secret"
            );
    }

    @Test
    @DisplayName("PostgreSQL constraint 이름은 보존하고 key detail tuple만 치환한다")
    void postgres_constraint_진단정보_보존() {
        String raw = """
            ERROR: duplicate key value violates unique constraint "uk_recruiting_application_email_key"
              Detail: Key (applicant_email, application_key)=(person@example.invalid, A1B2C3) already exists.
            """;

        String sanitized = ObservabilityErrorSanitizer.sanitizeMessage(raw);

        assertThat(sanitized)
            .contains("constraint \"uk_recruiting_application_email_key\"", "Detail: Key", "([REDACTED])")
            .doesNotContain("person@example.invalid", "A1B2C3");
    }

    @Test
    @DisplayName("따옴표 뒤에 긴 문자열이 이어져도 StackOverflowError 없이 정제한다")
    void 긴_메시지_스택오버플로_방지() {
        String longSingleQuoted = "prefix '" + "x".repeat(100_000);
        String longDoubleQuoted = "prefix \"" + "y".repeat(100_000);

        assertThatCode(() -> ObservabilityErrorSanitizer.sanitizeMessage(longSingleQuoted))
            .doesNotThrowAnyException();
        assertThatCode(() -> ObservabilityErrorSanitizer.sanitizeMessage(longDoubleQuoted))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("따옴표가 대량 반복돼도 스택을 소모하지 않고 정상 치환한다")
    void 반복_따옴표_스택오버플로_방지() {
        // 정규식 그룹 반복을 쓰던 시절 이 입력이 StackOverflowError 를 유발했다.
        String pathological = "'" + "''".repeat(100_000);

        String sanitized = ObservabilityErrorSanitizer.sanitizeMessage(pathological);

        assertThat(sanitized).contains(ObservabilityErrorSanitizer.REDACTED).doesNotContain("SANITIZE_FAILED");
    }

    @Test
    @DisplayName("정제 실패 마커는 민감값 치환과 구분된다")
    void 실패_마커와_치환_구분() {
        String redacted = ObservabilityErrorSanitizer.sanitizeMessage("email: person@example.invalid");

        assertThat(redacted).contains("[REDACTED]").doesNotContain("SANITIZE_FAILED");
    }

    @Test
    @DisplayName("원인 체인이 비정상적으로 깊어도 예외를 던지지 않고 진단 정보를 남긴다")
    void 깊은_원인_체인_실패_격리() {
        // 원인 체인 순회는 재귀 구조라 깊은 체인에서 스택이 바닥난다.
        StubException error = deepCauseChain(200_000);

        Throwable sanitized = ObservabilityErrorSanitizer.sanitize(error);

        assertThat(sanitized).isNotSameAs(error);
        assertThat(sanitized.getMessage())
            .contains("[SANITIZE_FAILED: ")
            .contains(StubException.class.getName())
            .doesNotContain("person@example.invalid");
        // 스택트레이스는 민감정보가 아니므로 실패해도 버리지 않는다.
        assertThat(sanitized.getStackTrace()).isEqualTo(error.getStackTrace());
    }

    private StubException deepCauseChain(int depth) {
        StubException error = new StubException("email: person@example.invalid");
        for (int index = 0; index < depth; index++) {
            StubException next = new StubException("level " + index);
            next.initCause(error);
            error = next;
        }
        return error;
    }

    /** 체인을 깊게 쌓아야 하므로 스택트레이스 수집 비용을 없앤다. */
    private static final class StubException extends RuntimeException {

        private StubException(String message) {
            super(message);
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    @Test
    @DisplayName("연속된 따옴표 이스케이프 경계에서도 기존 치환 결과를 유지한다")
    void 따옴표_이스케이프_경계() {
        assertThat(ObservabilityErrorSanitizer.sanitizeMessage("a 'he''llo' b"))
            .isEqualTo("a '[REDACTED]' b");
        assertThat(ObservabilityErrorSanitizer.sanitizeMessage("VALUES ('o''brien', 'x')"))
            .isEqualTo("VALUES ('[REDACTED]', '[REDACTED]')");
        // 닫히지 않은 따옴표는 남는다.
        assertThat(ObservabilityErrorSanitizer.sanitizeMessage("'a''"))
            .isEqualTo("'[REDACTED]''");
        assertThat(ObservabilityErrorSanitizer.sanitizeMessage("'unclosed"))
            .isEqualTo("'unclosed");
    }

    @Test
    @DisplayName("2중 따옴표 이스케이프 경계에서도 기존 치환 결과를 유지한다")
    void 이중따옴표_이스케이프_경계() {
        assertThat(ObservabilityErrorSanitizer.sanitizeMessage("a \"he\"\"llo\" b"))
            .isEqualTo("a \"[REDACTED]\" b");
        assertThat(ObservabilityErrorSanitizer.sanitizeMessage("VALUES (\"o\"\"brien\", \"x\")"))
            .isEqualTo("VALUES (\"[REDACTED]\", \"[REDACTED]\")");
        // 닫히지 않은 따옴표는 남는다.
        assertThat(ObservabilityErrorSanitizer.sanitizeMessage("\"a\"\""))
            .isEqualTo("\"[REDACTED]\"\"");
        assertThat(ObservabilityErrorSanitizer.sanitizeMessage("\"unclosed"))
            .isEqualTo("\"unclosed");
        // constraint 뒤 식별자는 진단 정보이므로 보존한다.
        assertThat(ObservabilityErrorSanitizer.sanitizeMessage("constraint \"uk_email\""))
            .isEqualTo("constraint \"uk_email\"");
    }
}
