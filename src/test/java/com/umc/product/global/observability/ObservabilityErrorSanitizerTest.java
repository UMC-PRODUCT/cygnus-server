package com.umc.product.global.observability;

import static org.assertj.core.api.Assertions.assertThat;

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
}
