package com.umc.product.community.application.port.in.command.thread.report.dto;

import java.util.Objects;

import com.umc.product.community.domain.enums.ReportReason;

public record ReportCommunityThreadMessageCommand(
    Long messageId,
    Long requesterMemberId,
    ReportReason reason
) {

    public ReportCommunityThreadMessageCommand {
        messageId = requirePositive(messageId, "messageId");
        requesterMemberId = requirePositive(requesterMemberId, "requesterMemberId");
        Objects.requireNonNull(reason, "reason must not be null");
    }

    private static Long requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
