package com.umc.product.community.adapter.in.web.dto.response;

import static com.umc.product.community.adapter.in.web.CommunityWebNumbers.text;

import java.time.Instant;

import com.umc.product.community.application.port.in.query.thread.report.dto.CommunityThreadMessageReportReceiptInfo;
import com.umc.product.community.domain.enums.ReportReason;

public record CommunityThreadMessageReportReceiptResponse(
    String reportId,
    String messageId,
    ReportReason reason,
    Instant createdAt
) {

    public static CommunityThreadMessageReportReceiptResponse from(
        CommunityThreadMessageReportReceiptInfo info
    ) {
        return new CommunityThreadMessageReportReceiptResponse(
            text(info.reportId()),
            text(info.messageId()),
            info.reason(),
            info.createdAt()
        );
    }
}
