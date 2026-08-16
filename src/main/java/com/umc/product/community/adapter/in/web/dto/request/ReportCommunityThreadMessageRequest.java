package com.umc.product.community.adapter.in.web.dto.request;

import com.umc.product.community.application.port.in.command.thread.report.dto.ReportCommunityThreadMessageCommand;
import com.umc.product.community.domain.enums.ReportReason;

import jakarta.validation.constraints.NotNull;

public record ReportCommunityThreadMessageRequest(
    @NotNull ReportReason reason
) {

    public ReportCommunityThreadMessageCommand toCommand(Long messageId, Long requesterMemberId) {
        return new ReportCommunityThreadMessageCommand(messageId, requesterMemberId, reason);
    }
}
