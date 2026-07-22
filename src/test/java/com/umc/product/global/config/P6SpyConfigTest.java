package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class P6SpyConfigTest {

    private static final String PROBE_EMAIL = "sql-probe@example.invalid";
    private static final String PROBE_KEY = "R4SAFE";

    private final RedactingP6SpyFormatter formatter = new RedactingP6SpyFormatter();

    @Test
    @DisplayName("prepared INSERT는 바인딩 값 대신 SQL 구조만 출력한다")
    void prepared_INSERT는_바인딩_값_대신_SQL_구조만_출력한다() {
        String prepared = """
            insert into recruiting_application (applicant_email, application_key, status)
            values (?, ?, ?)
            """;
        String bound = """
            insert into recruiting_application (applicant_email, application_key, status)
            values ('%s', '%s', 'DRAFT')
            """.formatted(PROBE_EMAIL, PROBE_KEY);

        String output = format(prepared, bound);

        assertThat(output)
            .contains("recruiting_application", "applicant_email", "application_key", "?")
            .doesNotContain(PROBE_EMAIL, PROBE_KEY, "DRAFT");
    }

    @Test
    @DisplayName("prepared SELECT는 조건의 이메일과 지원 키를 출력하지 않는다")
    void prepared_SELECT는_조건의_이메일과_지원_키를_출력하지_않는다() {
        String prepared = """
            select id from recruiting_application
            where applicant_email = ? and application_key = ?
            """;
        String bound = """
            select id from recruiting_application
            where applicant_email = '%s' and application_key = '%s'
            """.formatted(PROBE_EMAIL, PROBE_KEY);

        String output = format(prepared, bound);

        assertThat(output)
            .contains("recruiting_application", "applicant_email", "application_key", "?")
            .doesNotContain(PROBE_EMAIL, PROBE_KEY);
    }

    @Test
    @DisplayName("plain SQL 문자열 literal은 deterministic marker로 치환한다")
    void plain_SQL_문자열_literal은_deterministic_marker로_치환한다() {
        String sql = """
            select id from recruiting_application
            where applicant_email = '%s' and application_key = '%s'
            """.formatted(PROBE_EMAIL, PROBE_KEY);

        String output = format("", sql);

        assertThat(output)
            .contains("'[REDACTED]'")
            .doesNotContain(PROBE_EMAIL, PROBE_KEY);
    }

    @Test
    @DisplayName("비민감 SQL의 operation과 table 구조는 유지한다")
    void 비민감_SQL의_operation과_table_구조는_유지한다() {
        String output = format("select count(*) from recruiting_application", "select count(*) from recruiting_application");

        assertThat(output)
            .contains("select", "count(*)", "recruiting_application");
    }

    @Test
    @DisplayName("문자열 내부 주석 기호와 주석 내부 따옴표를 SQL token 경계에 맞게 치환한다")
    void 문자열과_주석의_token_경계를_보존한다() {
        String sql = """
            select '%s -- not a comment' as applicant_email,
                   application_key /* comment with '%s' */
            from recruiting_application
            """.formatted(PROBE_EMAIL, PROBE_KEY);

        String output = format("", sql);

        assertThat(output)
            .contains(
                "select",
                "'[REDACTED]' as applicant_email",
                "application_key /* [REDACTED] */",
                "from",
                "recruiting_application"
            )
            .doesNotContain(PROBE_EMAIL, PROBE_KEY);
    }

    @Test
    @DisplayName("blank SQL은 그대로 유지하고 prepared가 없으면 bound SQL을 사용한다")
    void blank와_bound_sql_fallback() {
        assertThat(SqlLogRedactor.redact(null, " ")).isBlank();
        assertThat(format("", null)).contains("null");
        assertThat(format("", "select 1")).contains("select", "1");
    }

    @Test
    @DisplayName("line comment와 dollar quoted 문자열도 민감값을 치환한다")
    void line_comment와_dollar_quote_redaction() {
        String redacted = SqlLogRedactor.redact("""
            select $$%s$$, $tag$%s$tag$ -- %s
            """.formatted(PROBE_EMAIL, PROBE_KEY, PROBE_EMAIL));

        assertThat(redacted)
            .contains("'[REDACTED]'", "-- [REDACTED]")
            .doesNotContain(PROBE_EMAIL, PROBE_KEY);
    }

    @Test
    @DisplayName("statement DDL과 statement가 아닌 category를 각각 형식화한다")
    void ddl과_non_statement_format() {
        assertThat(format("create table sample(id bigint)", ""))
            .containsIgnoringCase("create table");
        assertThat(format("alter table sample add name varchar(10)", ""))
            .containsIgnoringCase("alter table");
        assertThat(format("comment on table sample is 'secret'", ""))
            .containsIgnoringCase("comment on table")
            .doesNotContain("secret");
        assertThat(formatCategory("commit", "select 'secret'")).contains("'[REDACTED]'");
    }

    private String format(String prepared, String sql) {
        return formatter.formatMessage(1, "now", 3L, "statement", prepared, sql, "jdbc:postgresql:test");
    }

    private String formatCategory(String category, String sql) {
        return formatter.formatMessage(1, "now", 3L, category, "", sql, "jdbc:postgresql:test");
    }
}
