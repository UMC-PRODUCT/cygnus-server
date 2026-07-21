package com.umc.product.community.application.port.out.report.dto;

import com.umc.product.community.domain.enums.ReportReason;
import com.umc.product.community.domain.enums.ReportStatus;

public record ThreadMessageReportSearchQuery(
    ReportStatus status,
    ReportReason reason,
    Long threadId,
    Long reporterId,
    int offset,
    int limit
) {

    public ThreadMessageReportSearchQuery {
        threadId = requireOptionalPositive(threadId, "threadId");
        reporterId = requireOptionalPositive(reporterId, "reporterId");
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
    }
    private static Long requireOptionalPositive(Long value, String name) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
