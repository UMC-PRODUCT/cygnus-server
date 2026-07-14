package com.umc.product.audit.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLog;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.global.exception.constant.Domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "감사 로그 조회 응답")
public record AuditLogInfo(
    @Schema(description = "감사 로그 ID")
    Long id,
    @Schema(description = "감사 로그 도메인")
    Domain domain,
    @Schema(description = "감사 로그 액션")
    AuditAction action,
    @Schema(description = "감사 대상 타입")
    String targetType,
    @Schema(description = "감사 대상 식별자 문자열")
    String targetId,
    @Schema(description = "행위자 회원 ID", nullable = true)
    Long actorMemberId,
    @Schema(description = "감사 로그 설명", nullable = true)
    String description,
    @Schema(description = "JSON 문자열 형태의 감사 스냅샷. legacy row는 null 가능", nullable = true)
    String details,
    @Schema(description = "요청 IP", nullable = true)
    String ipAddress,
    @Schema(description = "감사 결과")
    AuditOutcome outcome,
    @Schema(description = "감사 로그 출처")
    AuditSource source,
    @Schema(description = "요청 식별자. legacy row는 null 가능", nullable = true)
    String requestId,
    @Schema(description = "분산 추적 식별자. legacy row는 null 가능", nullable = true)
    String traceId,
    @Schema(description = "감사 로그 생성 시각")
    Instant createdAt
) {
    public static AuditLogInfo from(AuditLog auditLog) {
        return new AuditLogInfo(
            auditLog.getId(),
            auditLog.getDomain(),
            auditLog.getAction(),
            auditLog.getTargetType(),
            auditLog.getTargetId(),
            auditLog.getActorMemberId(),
            auditLog.getDescription(),
            auditLog.getDetails(),
            auditLog.getIpAddress(),
            auditLog.getOutcome(),
            auditLog.getSource(),
            auditLog.getRequestId(),
            auditLog.getTraceId(),
            auditLog.getCreatedAt()
        );
    }
}
