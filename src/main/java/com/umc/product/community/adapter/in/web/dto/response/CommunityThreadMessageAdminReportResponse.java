package com.umc.product.community.adapter.in.web.dto.response;

import static com.umc.product.community.adapter.in.web.CommunityWebNumbers.text;

import java.time.Instant;

import com.umc.product.community.application.port.in.query.thread.report.dto.CommunityThreadMessageAdminReportInfo;
import com.umc.product.community.domain.enums.ReportReason;
import com.umc.product.community.domain.enums.ReportStatus;

public record CommunityThreadMessageAdminReportResponse(
    String reportId,
    String threadId,
    String messageId,
    ReportReason reason,
    ReportStatus status,
    String reporterId,
    Instant createdAt
) {

    public static CommunityThreadMessageAdminReportResponse from(
        CommunityThreadMessageAdminReportInfo info
    ) {
        return new CommunityThreadMessageAdminReportResponse(
            text(info.reportId()),
            text(info.threadId()),
            text(info.messageId()),
            info.reason(),
            info.status(),
            text(info.reporterId()),
            info.createdAt()
        );
    }
}
