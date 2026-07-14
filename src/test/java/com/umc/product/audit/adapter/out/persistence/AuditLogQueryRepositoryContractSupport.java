package com.umc.product.audit.adapter.out.persistence;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLog;
import com.umc.product.audit.domain.AuditLogEvent;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.global.exception.constant.Domain;

abstract class AuditLogQueryRepositoryContractSupport {

    protected static final Instant JANUARY_1 = Instant.parse("2026-01-01T00:00:00Z");
    protected static final Instant JANUARY_2 = Instant.parse("2026-01-02T00:00:00Z");
    protected static final Instant JANUARY_3 = Instant.parse("2026-01-03T00:00:00Z");
    protected static final Instant JANUARY_4 = Instant.parse("2026-01-04T00:00:00Z");

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    AuditLogJpaRepository auditLogJpaRepository;

    @Autowired
    AuditLogQueryRepository sut;

    @BeforeEach
    void setUpAuditRows() {
        persistAuditLog(Domain.MEMBER, AuditAction.CREATE, 10L, "member-create", JANUARY_1);
        persistAuditLog(Domain.MEMBER, AuditAction.UPDATE, 20L, "member-update", JANUARY_2);
        persistAuditLog(Domain.SCHEDULE, AuditAction.UPDATE, 10L, "schedule-update", JANUARY_3);
        persistAuditLog(Domain.MEMBER, AuditAction.UPDATE, 10L, "member-update-late", JANUARY_4);
        persistAuditLog(Domain.SCHEDULE, "schedule-target-10", 30L, AuditOutcome.FAILURE,
            AuditSource.AUTHORIZATION_ASPECT, "req-exact-10", "trace-exact-10", JANUARY_1.plusSeconds(60));
        persistAuditLog(Domain.SCHEDULE, "schedule-target-11", 31L, AuditOutcome.SUCCESS,
            AuditSource.EXPLICIT_RECORDER, "req-exact-11", "trace-exact-11", JANUARY_2.plusSeconds(60));
        persistAuditLog(Domain.SCHEDULE, "schedule-target-12", 32L, AuditOutcome.FAILURE,
            AuditSource.AUTHORIZATION_ASPECT, "req_%_literal", "trace_' OR '1'='1", JANUARY_3.plusSeconds(60));
        persistAuditLog(Domain.SCHEDULE, "schedule-target-13", 33L, AuditOutcome.FAILURE,
            AuditSource.AUTHORIZATION_ASPECT, "req-abc-literal", "trace-safe", JANUARY_4.plusSeconds(60));
        entityManager.clear();
    }

    private void persistAuditLog(
        Domain domain,
        AuditAction action,
        Long actorMemberId,
        String targetId,
        Instant createdAt
    ) {
        persistAuditLog(domain, action, "AuditContractTarget", targetId, actorMemberId,
            AuditOutcome.SUCCESS, AuditSource.ANNOTATION, null, null, createdAt);
    }

    private void persistAuditLog(
        Domain domain,
        String targetId,
        Long actorMemberId,
        AuditOutcome outcome,
        AuditSource source,
        String requestId,
        String traceId,
        Instant createdAt
    ) {
        persistAuditLog(domain, AuditAction.UPDATE, "Schedule", targetId, actorMemberId,
            outcome, source, requestId, traceId, createdAt);
    }

    private void persistAuditLog(
        Domain domain,
        AuditAction action,
        String targetType,
        String targetId,
        Long actorMemberId,
        AuditOutcome outcome,
        AuditSource source,
        String requestId,
        String traceId,
        Instant createdAt
    ) {
        AuditLog auditLog = auditLogJpaRepository.save(AuditLog.from(
            AuditLogEvent.builder()
                .occurredAt(createdAt)
                .domain(domain)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .actorMemberId(actorMemberId)
                .outcome(outcome)
                .source(source)
                .requestId(requestId)
                .traceId(traceId)
                .build(),
            null,
            "198.51.100.7"
        ));
        entityManager.flush();
        entityManager.getEntityManager()
            .createNativeQuery("UPDATE audit_log SET created_at = :createdAt WHERE id = :id")
            .setParameter("createdAt", Timestamp.from(createdAt))
            .setParameter("id", auditLog.getId())
            .executeUpdate();
    }
}
