package com.umc.product.audit.domain;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.umc.product.global.exception.constant.Domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 감사 로그 엔티티 (immutable, BaseEntity 미상속)
 */
@Entity
@Table(name = "audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Domain domain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;

    @Column(nullable = false, length = 100)
    private String targetType;

    @Column(length = 255)
    private String targetId;

    private Long actorMemberId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String details;

    @Column(length = 45)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditSource source;

    @Column(length = 100)
    private String requestId;

    @Column(length = 100)
    private String traceId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private AuditLog(
        Domain domain, AuditAction action, String targetType, String targetId,
        Long actorMemberId, String description, String details, String ipAddress,
        AuditOutcome outcome, AuditSource source, String requestId, String traceId
    ) {
        this.domain = domain;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.actorMemberId = actorMemberId;
        this.description = description;
        this.details = details;
        this.ipAddress = ipAddress;
        this.outcome = outcome;
        this.source = source;
        this.requestId = requestId;
        this.traceId = traceId;
    }

    public static AuditLog from(AuditLogEvent event, String detailsJson, String ipAddress) {
        return AuditLog.builder()
            .domain(event.domain())
            .action(event.action())
            .targetType(event.targetType())
            .targetId(event.targetId())
            .actorMemberId(event.actorMemberId())
            .description(AuditDescriptionPolicy.sanitize(event.description()))
            .details(detailsJson)
            .ipAddress(ipAddress)
            .outcome(event.outcome())
            .source(event.source())
            .requestId(event.requestId())
            .traceId(event.traceId())
            .build();
    }
}
