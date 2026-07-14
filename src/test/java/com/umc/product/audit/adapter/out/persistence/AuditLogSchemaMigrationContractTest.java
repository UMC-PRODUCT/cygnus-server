package com.umc.product.audit.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLog;
import com.umc.product.audit.domain.AuditLogEvent;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.EntityManager;

@PersistenceAdapterTest
@DisplayName("감사 로그 Flyway 스키마 계약")
class AuditLogSchemaMigrationContractTest {

    private static final List<String> PLANNED_INDEXES = List.of(
        "idx_audit_log_outcome_created_at",
        "idx_audit_log_source_created_at",
        "idx_audit_log_target_created_at",
        "idx_audit_log_request_id",
        "idx_audit_log_trace_id"
    );

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    AuditLogJpaRepository auditLogJpaRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("Flyway가 신규 컬럼의 타입과 기본값 및 null 계약을 적용한다")
    void Flyway가_신규_컬럼의_타입과_기본값_및_null_계약을_적용한다() {
        // when
        List<ColumnMetadata> columns = jdbcTemplate.query("""
            SELECT column_name, data_type, character_maximum_length, is_nullable, column_default
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'audit_log'
              AND column_name IN ('outcome', 'source', 'request_id', 'trace_id')
            ORDER BY column_name
            """, (resultSet, rowNumber) -> new ColumnMetadata(
                resultSet.getString("column_name"),
                resultSet.getString("data_type"),
                resultSet.getObject("character_maximum_length", Integer.class),
                resultSet.getString("is_nullable"),
                resultSet.getString("column_default")
            ));

        // then
        assertThat(columns).containsExactly(
            new ColumnMetadata("outcome", "character varying", 30, "NO", "'SUCCESS'::character varying"),
            new ColumnMetadata("request_id", "character varying", 100, "YES", null),
            new ColumnMetadata("source", "character varying", 50, "NO", "'ANNOTATION'::character varying"),
            new ColumnMetadata("trace_id", "character varying", 100, "YES", null)
        );
    }

    @Test
    @DisplayName("Flyway가 계획한 감사 로그 조회 인덱스를 모두 생성한다")
    void Flyway가_계획한_감사_로그_조회_인덱스를_모두_생성한다() {
        // when
        List<Map<String, Object>> indexes = jdbcTemplate.queryForList("""
            SELECT indexname, indexdef
            FROM pg_indexes
            WHERE schemaname = 'public'
              AND tablename = 'audit_log'
              AND indexname IN (
                  'idx_audit_log_outcome_created_at',
                  'idx_audit_log_source_created_at',
                  'idx_audit_log_target_created_at',
                  'idx_audit_log_request_id',
                  'idx_audit_log_trace_id'
              )
            ORDER BY indexname
            """);

        // then
        assertThat(indexes)
            .extracting(index -> index.get("indexname"))
            .containsExactlyElementsOf(PLANNED_INDEXES.stream().sorted().toList());
        assertThat(indexes)
            .extracting(index -> index.get("indexdef"))
            .allMatch(definition -> definition.toString().contains("USING btree"));
    }

    @Test
    @DisplayName("legacy insert는 신규 필드를 생략해도 DB 기본값으로 저장한다")
    void legacy_insert는_신규_필드를_생략해도_DB_기본값으로_저장한다() {
        // when
        Long id = jdbcTemplate.queryForObject("""
            INSERT INTO audit_log (domain, action, target_type, created_at)
            VALUES ('MEMBER', 'UPDATE', 'LegacyTarget', CURRENT_TIMESTAMP)
            RETURNING id
            """, Long.class);
        Map<String, Object> stored = jdbcTemplate.queryForMap("""
            SELECT outcome, source, request_id, trace_id
            FROM audit_log
            WHERE id = ?
            """, id);

        // then
        assertThat(stored)
            .containsEntry("outcome", "SUCCESS")
            .containsEntry("source", "ANNOTATION")
            .containsEntry("request_id", null)
            .containsEntry("trace_id", null);
    }

    @Test
    @DisplayName("신규 enum과 요청 추적값은 PostgreSQL과 JPA 사이를 왕복한다")
    void 신규_enum과_요청_추적값은_PostgreSQL과_JPA_사이를_왕복한다() {
        // given
        AuditLog auditLog = AuditLog.from(
            AuditLogEvent.builder()
                .domain(Domain.AUTHENTICATION)
                .action(AuditAction.LOGIN)
                .targetType("Authentication")
                .outcome(AuditOutcome.FAILURE)
                .source(AuditSource.SYSTEM)
                .requestId("request-round-trip")
                .traceId("trace-round-trip")
                .build(),
            null,
            "198.51.100.7"
        );

        // when
        Long id = auditLogJpaRepository.saveAndFlush(auditLog).getId();
        entityManager.clear();
        AuditLog stored = auditLogJpaRepository.findById(id).orElseThrow();

        // then
        assertThat(stored.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(stored.getSource()).isEqualTo(AuditSource.SYSTEM);
        assertThat(stored.getRequestId()).isEqualTo("request-round-trip");
        assertThat(stored.getTraceId()).isEqualTo("trace-round-trip");
    }

    @Test
    @DisplayName("지원하지 않는 outcome 문자열은 JPA enum 변환에서 명시적으로 실패한다")
    void 지원하지_않는_outcome_문자열은_JPA_enum_변환에서_명시적으로_실패한다() {
        // given
        Long id = jdbcTemplate.queryForObject("""
            INSERT INTO audit_log (domain, action, target_type, outcome, created_at)
            VALUES ('AUTHENTICATION', 'LOGIN', 'Authentication', 'NOT_SUPPORTED', CURRENT_TIMESTAMP)
            RETURNING id
            """, Long.class);
        entityManager.clear();

        // when
        Throwable thrown = catchThrowable(() -> auditLogJpaRepository.findById(id));

        // then
        assertThat(thrown)
            .isNotNull()
            .hasRootCauseInstanceOf(IllegalArgumentException.class)
            .hasStackTraceContaining("No enum constant")
            .hasStackTraceContaining("AuditOutcome.NOT_SUPPORTED");
    }

    @Test
    @DisplayName("fresh PostgreSQL에는 신규 Flyway migration 성공 이력이 존재한다")
    void fresh_PostgreSQL에는_신규_Flyway_migration_성공_이력이_존재한다() {
        // when
        Map<String, Object> migration = jdbcTemplate.queryForMap("""
            SELECT version, description, success
            FROM flyway_schema_history
            WHERE description = 'extend audit log schema'
            """);

        // then
        assertThat(migration).containsEntry("success", true);
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable result = throwable;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }

    private record ColumnMetadata(
        String name,
        String dataType,
        Integer maximumLength,
        String nullable,
        String defaultValue
    ) {
    }
}
