package com.umc.product.global.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.micrometer.tracing.Span;

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
    @DisplayName("suppressed 예외의 민감 메시지까지 원본을 변경하지 않고 복제·정제한다")
    void suppressed_sensitive_error_copy() {
        IllegalStateException outer = new IllegalStateException("outer");
        outer.addSuppressed(new IllegalArgumentException("email=person@example.invalid"));

        Throwable sanitized = ObservabilityErrorSanitizer.sanitize(outer);

        assertThat(sanitized).isNotSameAs(outer);
        assertThat(sanitized.getSuppressed()).hasSize(1);
        assertThat(sanitized.getSuppressed()[0].getMessage()).contains("[REDACTED]");
        assertThat(outer.getSuppressed()[0].getMessage()).contains("person@example.invalid");
    }

    @Test
    @DisplayName("순환 cause graph도 무한 재귀 없이 한 번씩 복제한다")
    void cyclic_cause_graph() {
        RuntimeException first = new RuntimeException("person@example.invalid");
        RuntimeException second = new RuntimeException("second");
        first.initCause(second);
        second.initCause(first);

        Throwable sanitized = ObservabilityErrorSanitizer.sanitize(first);

        assertThat(sanitized.getCause().getCause()).isSameAs(sanitized);
    }

    @Test
    @DisplayName("suppressed SQLException과 constraint에서 DB 진단 metadata를 추출한다")
    void suppressed_sql_metadata() {
        Span span = mock(Span.class);
        IllegalStateException outer = new IllegalStateException("outer");
        outer.addSuppressed(new SQLException(
            "duplicate constraint \"uk_member_email\" email=person@example.invalid",
            "23505",
            7
        ));

        ObservabilityErrorSanitizer.record(span, outer);

        then(span).should().tag("db.response.sql_state", "23505");
        then(span).should().tag("db.response.vendor_code", "7");
        then(span).should().tag("db.constraint.name", "uk_member_email");
    }

    @Test
    @DisplayName("SQL 예외가 없는 null·일반 예외는 DB tag 없이 안전하게 기록한다")
    void null_and_plain_error_metadata() {
        Span span = mock(Span.class);

        ObservabilityErrorSanitizer.record(span, null);
        ObservabilityErrorSanitizer.record(span, new IllegalStateException("plain"));

        then(span).should().tag("app.error.class", "unknown");
        then(span).should().tag("app.error.class", IllegalStateException.class.getName());
    }
}
