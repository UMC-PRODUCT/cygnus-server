package com.umc.product.audit.adapter.out.persistence;

import static com.umc.product.audit.domain.QAuditLog.auditLog;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLog;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.global.exception.constant.Domain;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AuditLogQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<AuditLog> search(
        Domain domain, AuditAction action, Long actorMemberId,
        Instant from, Instant to, String targetType, String targetId,
        AuditOutcome outcome, AuditSource source, String requestId, String traceId,
        Pageable pageable
    ) {
        List<AuditLog> content = queryFactory
            .selectFrom(auditLog)
            .where(
                domainEq(domain),
                actionEq(action),
                actorMemberIdEq(actorMemberId),
                createdAtGoe(from),
                createdAtLoe(to),
                targetTypeEq(targetType),
                targetIdEq(targetId),
                outcomeEq(outcome),
                sourceEq(source),
                requestIdEq(requestId),
                traceIdEq(traceId)
            )
            .orderBy(auditLog.createdAt.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = queryFactory
            .select(auditLog.count())
            .from(auditLog)
            .where(
                domainEq(domain),
                actionEq(action),
                actorMemberIdEq(actorMemberId),
                createdAtGoe(from),
                createdAtLoe(to),
                targetTypeEq(targetType),
                targetIdEq(targetId),
                outcomeEq(outcome),
                sourceEq(source),
                requestIdEq(requestId),
                traceIdEq(traceId)
            )
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private BooleanExpression domainEq(Domain domain) {
        return domain == null ? null : auditLog.domain.eq(domain);
    }

    private BooleanExpression actionEq(AuditAction action) {
        return action == null ? null : auditLog.action.eq(action);
    }

    private BooleanExpression actorMemberIdEq(Long actorMemberId) {
        return actorMemberId == null ? null : auditLog.actorMemberId.eq(actorMemberId);
    }

    private BooleanExpression createdAtGoe(Instant from) {
        return from == null ? null : auditLog.createdAt.goe(from);
    }

    private BooleanExpression createdAtLoe(Instant to) {
        return to == null ? null : auditLog.createdAt.loe(to);
    }

    private BooleanExpression targetTypeEq(String targetType) {
        return targetType == null ? null : auditLog.targetType.eq(targetType);
    }

    private BooleanExpression targetIdEq(String targetId) {
        return targetId == null ? null : auditLog.targetId.eq(targetId);
    }

    private BooleanExpression outcomeEq(AuditOutcome outcome) {
        return outcome == null ? null : auditLog.outcome.eq(outcome);
    }

    private BooleanExpression sourceEq(AuditSource source) {
        return source == null ? null : auditLog.source.eq(source);
    }

    private BooleanExpression requestIdEq(String requestId) {
        return requestId == null ? null : auditLog.requestId.eq(requestId);
    }

    private BooleanExpression traceIdEq(String traceId) {
        return traceId == null ? null : auditLog.traceId.eq(traceId);
    }
}
