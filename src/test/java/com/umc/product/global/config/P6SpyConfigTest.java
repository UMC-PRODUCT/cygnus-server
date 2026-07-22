package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Properties;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.Query;

import com.umc.product.global.event.adapter.out.persistence.EventOutboxJpaRepository;

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
    @DisplayName("P6Spy는 명시적인 제외 tag가 붙은 event outbox polling SQL만 로그에서 제외한다")
    void P6Spy는_명시적인_제외_tag가_붙은_event_outbox_polling_SQL만_로그에서_제외한다()
        throws NoSuchMethodException {
        Pattern logFilter = loadSqlLogFilter();
        Query pollingQuery = EventOutboxJpaRepository.class
            .getMethod("findPublishableForUpdate", int.class, Instant.class)
            .getAnnotation(Query.class);
        String pollingSql = pollingQuery.value();

        assertThat(pollingSql).contains("/* p6spy:exclude */");
        assertThat(logFilter.matcher(pollingSql).matches()).isFalse();
        assertThat(logFilter.matcher(pollingSql.replace("/* p6spy:exclude */", "")).matches()).isTrue();
        assertThat(logFilter.matcher("select * from member where id = ?").matches()).isTrue();
    }

    private Pattern loadSqlLogFilter() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties properties = yaml.getObject();
        assertThat(properties).isNotNull();
        return Pattern.compile(properties.getProperty("decorator.datasource.p6spy.log-filter.pattern"));
    }

    private String format(String prepared, String sql) {
        return formatter.formatMessage(1, "now", 3L, "statement", prepared, sql, "jdbc:postgresql:test");
    }
}
