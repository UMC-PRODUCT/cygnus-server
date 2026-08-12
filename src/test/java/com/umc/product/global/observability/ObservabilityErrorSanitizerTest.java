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
    @DisplayName("정제가 실패해도 예외를 던지지 않고 원문 대신 실패 마커를 반환한다")
    void 정제_실패_시_원문_비노출() {
        // `''` 반복은 그룹 반복의 재귀 깊이를 직접 늘려 StackOverflowError 를 유발한다.
        String pathological = "'" + "''".repeat(100_000);

        String sanitized = ObservabilityErrorSanitizer.sanitizeMessage(pathological);

        assertThat(sanitized)
            .startsWith("[SANITIZE_FAILED: ")
            .contains("StackOverflowError")
            .contains("length=" + pathological.length());
    }

    @Test
    @DisplayName("정제 실패 마커는 민감값 치환과 구분된다")
    void 실패_마커와_치환_구분() {
        String redacted = ObservabilityErrorSanitizer.sanitizeMessage("email: person@example.invalid");

        assertThat(redacted).contains("[REDACTED]").doesNotContain("SANITIZE_FAILED");
    }

    @Test
    @DisplayName("예외 정제가 실패해도 원본 메시지를 노출하지 않는다")
    void 예외_정제_실패_시_원문_비노출() {
        IllegalStateException error = new IllegalStateException("'" + "''".repeat(100_000));

        Throwable sanitized = ObservabilityErrorSanitizer.sanitize(error);

        assertThat(sanitized).isNotSameAs(error);
        assertThat(sanitized.getMessage()).doesNotContain("''''");
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
