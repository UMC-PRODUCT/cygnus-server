package com.umc.product.audit.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.global.exception.constant.Domain;

public record SearchAuditLogQuery(
    Domain domain,
    AuditAction action,
    Long actorMemberId,
    Instant from,
    Instant to,
    String targetType,
    String targetId,
    AuditOutcome outcome,
    AuditSource source,
    String requestId,
    String traceId
) {
}
