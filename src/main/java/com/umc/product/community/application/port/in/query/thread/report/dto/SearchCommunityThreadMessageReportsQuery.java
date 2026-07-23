package com.umc.product.community.application.port.in.query.thread.report.dto;

import com.umc.product.community.domain.enums.ReportReason;
import com.umc.product.community.domain.enums.ReportStatus;

public record SearchCommunityThreadMessageReportsQuery(
    Long requesterMemberId,
    ReportStatus status,
    ReportReason reason,
    Long threadId,
    Long reporterMemberId,
    int offset,
    int limit
) {

    public SearchCommunityThreadMessageReportsQuery {
        requesterMemberId = requirePositive(requesterMemberId, "requesterMemberId");
        threadId = requireOptionalPositive(threadId, "threadId");
        reporterMemberId = requireOptionalPositive(reporterMemberId, "reporterMemberId");
        requirePage(offset, limit);
    }

    public Long reporterId() {
        return reporterMemberId;
    }

    private static void requirePage(int offset, int limit) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
    }

    private static Long requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static Long requireOptionalPositive(Long value, String name) {
        return value == null ? null : requirePositive(value, name);
    }
}
