package com.umc.product.community.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.community.domain.enums.ReportReason;
import com.umc.product.community.domain.enums.ReportStatus;
import com.umc.product.community.domain.enums.ReportTargetType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(length = 500)
    private String reason;

    @Column(name = "thread_id")
    private Long threadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 32)
    private ReportReason reasonCode;

    @Builder
    private Report(
        Long reporterId,
        ReportTargetType targetType,
        Long targetId,
        String reason,
        Long threadId,
        ReportReason reasonCode
    ) {
        this.reporterId = reporterId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.status = ReportStatus.PENDING;
        this.reason = reason;
        this.threadId = threadId;
        this.reasonCode = reasonCode;
    }

    public static Report create(Long reporterId, ReportTargetType targetType, Long targetId, String reason) {
        if (targetType == ReportTargetType.THREAD_MESSAGE) {
            throw new IllegalArgumentException("THREAD_MESSAGE reports require threadId and reasonCode");
        }
        return Report.builder()
            .reporterId(reporterId)
            .targetType(targetType)
            .targetId(targetId)
            .reason(reason)
            .build();
    }

    public static Report createThreadMessage(
        Long reporterId,
        Long threadId,
        Long messageId,
        ReportReason reason
    ) {
        return Report.builder()
            .reporterId(requirePositive(reporterId, "reporterId"))
            .targetType(ReportTargetType.THREAD_MESSAGE)
            .targetId(requirePositive(messageId, "messageId"))
            .threadId(requirePositive(threadId, "threadId"))
            .reasonCode(requireReason(reason))
            .build();
    }

    public void approve() {
        this.status = ReportStatus.APPROVED;
    }

    public void reject() {
        this.status = ReportStatus.REJECTED;
    }

    private static Long requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static ReportReason requireReason(ReportReason reason) {
        if (reason == null) {
            throw new IllegalArgumentException("reason must not be null");
        }
        return reason;
    }
}
