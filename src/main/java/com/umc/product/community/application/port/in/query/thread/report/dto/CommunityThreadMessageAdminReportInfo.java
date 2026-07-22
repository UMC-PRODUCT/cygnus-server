package com.umc.product.community.application.port.in.query.thread.report.dto;

import java.time.Instant;

import com.umc.product.community.domain.Report;
import com.umc.product.community.domain.enums.ReportReason;
import com.umc.product.community.domain.enums.ReportStatus;

public record CommunityThreadMessageAdminReportInfo(
    Long reportId,
    Long threadId,
    Long messageId,
    ReportReason reason,
    ReportStatus status,
    Long reporterId,
    Instant createdAt
) {

    public static CommunityThreadMessageAdminReportInfo from(Report report) {
        return new CommunityThreadMessageAdminReportInfo(
            report.getId(),
            report.getThreadId(),
            report.getTargetId(),
            report.getReasonCode(),
            report.getStatus(),
            report.getReporterId(),
            report.getCreatedAt()
        );
    }
}
