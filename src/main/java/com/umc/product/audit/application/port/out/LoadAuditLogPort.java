package com.umc.product.audit.application.port.out;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLog;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.global.exception.constant.Domain;

public interface LoadAuditLogPort {
    Page<AuditLog> search(
        Domain domain, AuditAction action, Long actorMemberId,
        Instant from, Instant to, String targetType, String targetId,
        AuditOutcome outcome, AuditSource source, String requestId, String traceId,
        Pageable pageable
    );
}
