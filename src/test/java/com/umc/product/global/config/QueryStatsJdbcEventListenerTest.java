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
