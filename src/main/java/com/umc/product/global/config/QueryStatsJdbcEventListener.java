package com.umc.product.global.config;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.p6spy.engine.common.PreparedStatementInformation;
import com.p6spy.engine.common.StatementInformation;
import com.p6spy.engine.event.JdbcEventListener;
import com.umc.product.global.observability.ObservabilityErrorSanitizer;
import com.umc.product.global.observability.ObservabilityTracingProperties;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

/**
 * P6Spy JdbcEventListener를 통해 요청 단위 쿼리 통계를 수집합니다. p6spy-spring-boot-starter가 @Component JdbcEventListener 빈을 자동 등록합니다.
 *
 * <p>성공한 쿼리만 통계({@link QueryStatsHolder})에 누적합니다 (실패한 쿼리는 별도 추적하지 않음).
 * SQL 원문 라인 로깅은 P6Spy 자체 출력({@link P6SpyConfig})이 담당하며, 본 리스너는 라인 로깅 책임을 갖지 않습니다 —
 * {@code decorator.datasource.p6spy.enable-logging=false} 로 라인 로깅을 꺼도 {@code queryCount}/{@code queryTimeMs} 집계는 그대로 동작합니다.
 */
@Component
public class QueryStatsJdbcEventListener extends JdbcEventListener {

    private static final Pattern SQL_OPERATION_PATTERN = Pattern.compile(
        "^\\s*(?:/\\*.*?\\*/\\s*)*(\\w+)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final String DB_SYSTEM = "postgresql";

    private final Tracer tracer;
    private final ObservabilityTracingProperties tracingProperties;
    private final ThreadLocal<Deque<DbQuerySpanContext>> dbSpanStack = ThreadLocal.withInitial(ArrayDeque::new);

    @Autowired
    public QueryStatsJdbcEventListener(
        ObjectProvider<Tracer> tracerProvider,
        ObservabilityTracingProperties tracingProperties
    ) {
        this(tracerProvider.getIfAvailable(() -> Tracer.NOOP), tracingProperties);
    }

    QueryStatsJdbcEventListener(Tracer tracer, ObservabilityTracingProperties tracingProperties) {
        this.tracer = tracer;
        this.tracingProperties = tracingProperties;
    }

    @Override
    public void onBeforeExecute(PreparedStatementInformation info) {
        startDbSpan(info, "statement");
    }

    @Override
    public void onAfterExecute(
        PreparedStatementInformation info, long timeElapsedNanos, SQLException e
    ) {
        completeDbSpan(timeElapsedNanos, e);
        record(timeElapsedNanos, e);
    }

    @Override
    public void onBeforeExecuteUpdate(PreparedStatementInformation info) {
        startDbSpan(info, "update");
    }

    @Override
    public void onAfterExecuteQuery(
        PreparedStatementInformation info, long timeElapsedNanos, SQLException e
    ) {
        completeDbSpan(timeElapsedNanos, e);
        record(timeElapsedNanos, e);
    }

    @Override
    public void onBeforeExecuteQuery(PreparedStatementInformation info) {
        startDbSpan(info, "query");
    }

    @Override
    public void onAfterExecuteUpdate(
        PreparedStatementInformation info, long timeElapsedNanos, int rowCount,
        SQLException e
    ) {
        completeDbSpan(timeElapsedNanos, e);
        record(timeElapsedNanos, e);
    }

    @Override
    public void onBeforeExecuteBatch(StatementInformation info) {
        startDbSpan(info, "batch");
    }

    @Override
    public void onAfterExecuteBatch(
        StatementInformation info, long timeElapsedNanos, int[] updateCounts,
        SQLException e
    ) {
        completeDbSpan(timeElapsedNanos, e);
        record(timeElapsedNanos, e);
    }

    @Override
    public void onBeforeExecute(StatementInformation info, String sql) {
        info.setStatementQuery(sql);
        startDbSpan(info, "statement");
    }

    @Override
    public void onAfterExecute(
        StatementInformation info, long timeElapsedNanos, String sql, SQLException e
    ) {
        completeDbSpan(timeElapsedNanos, e);
        record(timeElapsedNanos, e);
    }

    @Override
    public void onBeforeExecuteQuery(StatementInformation info, String sql) {
        info.setStatementQuery(sql);
        startDbSpan(info, "query");
    }

    @Override
    public void onAfterExecuteQuery(
        StatementInformation info, long timeElapsedNanos, String sql, SQLException e
    ) {
        completeDbSpan(timeElapsedNanos, e);
        record(timeElapsedNanos, e);
    }

    @Override
    public void onBeforeExecuteUpdate(StatementInformation info, String sql) {
        info.setStatementQuery(sql);
        startDbSpan(info, "update");
    }

    @Override
    public void onAfterExecuteUpdate(
        StatementInformation info, long timeElapsedNanos, String sql, int rowCount, SQLException e
    ) {
        completeDbSpan(timeElapsedNanos, e);
        record(timeElapsedNanos, e);
    }

    private void record(long timeElapsedNanos, SQLException e) {
        if (e == null) {
            QueryStatsHolder.record(timeElapsedNanos);
        }
    }

    private void startDbSpan(StatementInformation info, String statementType) {
        if (!tracingProperties.isEnabled() || !tracingProperties.isDbSpans()) {
            return;
        }

        String sql = info.getSql();
        String operation = resolveOperation(sql);
        Span span = tracer.nextSpan()
            .name("db.%s".formatted(statementType))
            .tag("app.layer", "db")
            .tag("db.system", DB_SYSTEM)
            .tag("db.operation", operation)
            .tag("db.statement.type", statementType)
            .start();

        if (tracingProperties.isIncludeSql()) {
            span.tag("db.statement", limitSql(SqlLogRedactor.redact(sql)));
        }

        Tracer.SpanInScope scope = tracer.withSpan(span);
        dbSpanStack.get().push(new DbQuerySpanContext(span, scope));
    }

    private void completeDbSpan(long timeElapsedNanos, SQLException e) {
        Deque<DbQuerySpanContext> contexts = dbSpanStack.get();
        if (contexts.isEmpty()) {
            clearIfEmpty(contexts);
            return;
        }

        DbQuerySpanContext context = contexts.pop();
        try {
            context.span().tag("db.query.elapsed_ms", String.valueOf(timeElapsedNanos / 1_000_000L));
            if (e != null) {
                ObservabilityErrorSanitizer.record(context.span(), e);
            }
        } finally {
            context.scope().close();
            context.span().end();
            clearIfEmpty(contexts);
        }
    }

    private void clearIfEmpty(Deque<DbQuerySpanContext> contexts) {
        if (contexts.isEmpty()) {
            dbSpanStack.remove();
        }
    }

    private String resolveOperation(String sql) {
        if (sql == null || sql.isBlank()) {
            return "UNKNOWN";
        }

        Matcher matcher = SQL_OPERATION_PATTERN.matcher(sql);
        if (!matcher.find()) {
            return "UNKNOWN";
        }
        return matcher.group(1).toUpperCase(Locale.ROOT);
    }

    private String limitSql(String sql) {
        if (sql == null) {
            return "";
        }

        int maxSqlLength = Math.max(0, tracingProperties.getMaxSqlLength());
        if (sql.length() <= maxSqlLength) {
            return sql;
        }
        return sql.substring(0, maxSqlLength);
    }

    private record DbQuerySpanContext(Span span, Tracer.SpanInScope scope) {
    }
}
