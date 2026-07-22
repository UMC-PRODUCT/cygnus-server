package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.p6spy.engine.common.PreparedStatementInformation;
import com.p6spy.engine.common.StatementInformation;
import com.umc.product.global.observability.ObservabilityTracingProperties;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

class QueryStatsJdbcEventListenerTest {

    private static final String PROBE_EMAIL = "span-probe@example.invalid";
    private static final String PROBE_KEY = "R4S8F2";
    private static final String CONSTRAINT_NAME = "uk_recruiting_application_email_key";

    private Tracer tracer;
    private Span span;
    private Tracer.SpanInScope spanInScope;
    private QueryStatsJdbcEventListener sut;

    @BeforeEach
    void setUp() {
        tracer = mock(Tracer.class);
        span = mock(Span.class);
        spanInScope = mock(Tracer.SpanInScope.class);

        given(tracer.nextSpan()).willReturn(span);
        given(tracer.withSpan(span)).willReturn(spanInScope);
        given(span.name(org.mockito.ArgumentMatchers.anyString())).willReturn(span);
        given(span.tag(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
            .willReturn(span);
        given(span.start()).willReturn(span);

        sut = new QueryStatsJdbcEventListener(tracer, new ObservabilityTracingProperties());
    }

    @AfterEach
    void tearDown() {
        QueryStatsHolder.clear();
    }

    @Test
    @DisplayName("DB 쿼리 실행을 child span으로 남기고 요청 단위 쿼리 통계를 기록한다")
    void db_쿼리_span_및_요청_쿼리_통계_기록() {
        PreparedStatementInformation info = mock(PreparedStatementInformation.class);
        given(info.getSql()).willReturn("select * from member where id = ?");

        QueryStatsHolder.init();

        sut.onBeforeExecuteQuery(info);
        sut.onAfterExecuteQuery(info, 12_500_000L, null);

        assertThat(QueryStatsHolder.getQueryCount()).isEqualTo(1L);
        assertThat(QueryStatsHolder.getTotalTimeMs()).isEqualTo(12L);
        then(span).should().name("db.query");
        then(span).should().tag("db.system", "postgresql");
        then(span).should().tag("db.operation", "SELECT");
        then(span).should().tag("db.query.elapsed_ms", "12");
        then(span).should().end();
        then(spanInScope).should().close();
    }

    @Test
    @DisplayName("SQL 앞에 주석이 있어도 실제 DB operation을 기록한다")
    void sql_앞_주석_무시하고_operation_기록() {
        PreparedStatementInformation info = mock(PreparedStatementInformation.class);
        given(info.getSql()).willReturn("/* trace-id: abc */\nselect * from member where id = ?");

        sut.onBeforeExecuteQuery(info);
        sut.onAfterExecuteQuery(info, 1_000_000L, null);

        then(span).should().tag("db.operation", "SELECT");
    }

    @Test
    @DisplayName("DB span에 SQL을 포함해도 문자열 literal은 노출하지 않는다")
    void db_span_SQL_문자열_literal_redaction() {
        ObservabilityTracingProperties properties = new ObservabilityTracingProperties();
        properties.setIncludeSql(true);
        sut = new QueryStatsJdbcEventListener(tracer, properties);
        PreparedStatementInformation info = mock(PreparedStatementInformation.class);
        given(info.getSql()).willReturn(
            "select id from recruiting_application where applicant_email = 'sql-span@example.invalid'"
        );

        sut.onBeforeExecuteQuery(info);
        sut.onAfterExecuteQuery(info, 1_000_000L, null);

        then(span).should().tag(
            "db.statement",
            "select id from recruiting_application where applicant_email = '[REDACTED]'"
        );
    }

    @Test
    @DisplayName("민감 DB 쿼리 실패는 span에서 값만 치환하고 진단 메타데이터를 유지한다")
    void 민감_DB_쿼리_실패_span_error_redaction() {
        PreparedStatementInformation info = mock(PreparedStatementInformation.class);
        SQLException exception = duplicateException();

        QueryStatsHolder.init();

        sut.onBeforeExecuteQuery(info);
        sut.onAfterExecuteQuery(info, 3_000_000L, exception);

        assertThat(QueryStatsHolder.getQueryCount()).isZero();
        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        then(span).should().error(errorCaptor.capture());
        assertThat(errorCaptor.getValue())
            .isNotSameAs(exception)
            .hasMessageContaining(SQLException.class.getName())
            .hasMessageContaining("sqlState=23505")
            .hasMessageContaining("vendorCode=0")
            .hasMessageContaining("constraint=" + CONSTRAINT_NAME)
            .hasMessageNotContaining(PROBE_EMAIL)
            .hasMessageNotContaining(PROBE_KEY)
            .hasMessageNotContaining("=(42,");
        then(span).should().tag("app.error.class", SQLException.class.getName());
        then(span).should().tag("db.error.class", SQLException.class.getName());
        then(span).should().tag("db.response.sql_state", "23505");
        then(span).should().tag("db.response.vendor_code", "0");
        then(span).should().tag("db.constraint.name", CONSTRAINT_NAME);
        then(span).should().tag("db.query.elapsed_ms", "3");
        then(span).should().end();
        then(spanInScope).should().close();
    }

    @Test
    @DisplayName("P6Spy의 prepared·statement 실행 변형을 모두 동일하게 집계한다")
    void p6spy_execute_callback_variants() {
        ObservabilityTracingProperties properties = new ObservabilityTracingProperties();
        properties.setEnabled(false);
        sut = new QueryStatsJdbcEventListener(tracer, properties);
        PreparedStatementInformation prepared = mock(PreparedStatementInformation.class);
        StatementInformation statement = mock(StatementInformation.class);
        QueryStatsHolder.init();

        sut.onBeforeExecute(prepared);
        sut.onAfterExecute(prepared, 1_000_000L, null);
        sut.onBeforeExecuteUpdate(prepared);
        sut.onAfterExecuteUpdate(prepared, 2_000_000L, 1, null);
        sut.onBeforeExecuteBatch(statement);
        sut.onAfterExecuteBatch(statement, 3_000_000L, new int[]{1}, null);
        sut.onBeforeExecute(statement, "select 1");
        sut.onAfterExecute(statement, 4_000_000L, "select 1", null);
        sut.onBeforeExecuteQuery(statement, "select 1");
        sut.onAfterExecuteQuery(statement, 5_000_000L, "select 1", null);
        sut.onBeforeExecuteUpdate(statement, "update sample set value = 1");
        sut.onAfterExecuteUpdate(statement, 6_000_000L, "update sample set value = 1", 1, null);

        assertThat(QueryStatsHolder.getQueryCount()).isEqualTo(6);
        assertThat(QueryStatsHolder.getTotalTimeMs()).isEqualTo(21);
        then(statement).should(org.mockito.Mockito.atLeastOnce()).setStatementQuery("select 1");
        then(statement).should().setStatementQuery("update sample set value = 1");
    }

    @Test
    @DisplayName("span 없이 after callback만 수신해도 stack을 정리하고 성공 쿼리만 기록한다")
    void after_without_before와_실패_query() {
        QueryStatsHolder.init();
        PreparedStatementInformation info = mock(PreparedStatementInformation.class);

        sut.onAfterExecute(info, 1_000_000L, null);
        sut.onAfterExecute(info, 1_000_000L, new SQLException("failure"));

        assertThat(QueryStatsHolder.getQueryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("null·해석 불가 SQL은 UNKNOWN이며 span SQL 길이 제한을 적용한다")
    void unknown_operation과_sql_length_limit() {
        ObservabilityTracingProperties properties = new ObservabilityTracingProperties();
        properties.setIncludeSql(true);
        properties.setMaxSqlLength(5);
        sut = new QueryStatsJdbcEventListener(tracer, properties);
        PreparedStatementInformation nullSql = mock(PreparedStatementInformation.class);
        PreparedStatementInformation invalidSql = mock(PreparedStatementInformation.class);
        PreparedStatementInformation longSql = mock(PreparedStatementInformation.class);
        given(invalidSql.getSql()).willReturn("/* comment only */");
        given(longSql.getSql()).willReturn("select * from member");

        sut.onBeforeExecuteQuery(nullSql);
        sut.onAfterExecuteQuery(nullSql, 1L, null);
        sut.onBeforeExecuteQuery(invalidSql);
        sut.onAfterExecuteQuery(invalidSql, 1L, null);
        sut.onBeforeExecuteQuery(longSql);
        sut.onAfterExecuteQuery(longSql, 1L, null);

        then(span).should(org.mockito.Mockito.atLeast(2)).tag("db.operation", "UNKNOWN");
        then(span).should().tag("db.statement", "");
        then(span).should().tag("db.statement", "selec");
    }

    @Test
    @DisplayName("QueryStatsHolder는 초기화 전 record를 무시하고 기본값을 반환한다")
    void query_stats_holder_noop_before_init() {
        new QueryStatsHolder();
        QueryStatsHolder.clear();

        QueryStatsHolder.record(10_000_000L);

        assertThat(QueryStatsHolder.getQueryCount()).isZero();
        assertThat(QueryStatsHolder.getTotalTimeMs()).isZero();
    }

    private SQLException duplicateException() {
        return new SQLException(
            """
                ERROR: duplicate key value violates unique constraint "%s"
                  Detail: Key (recruiting_round_id, applicant_email, application_key)=(42, %s, %s) already exists.
                """.formatted(CONSTRAINT_NAME, PROBE_EMAIL, PROBE_KEY),
            "23505",
            0
        );
    }
}
