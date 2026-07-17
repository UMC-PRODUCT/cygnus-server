package com.umc.product.inquiry.adapter.in.web.dto.request;

import com.umc.product.inquiry.application.port.in.command.dto.AssignInquiryManagerCommand;

import jakarta.validation.constraints.NotNull;

public record AssignInquiryManagerRequest(
    @NotNull(message = "지정할 담당자(targetManagerId)는 필수입니다.")
    Long targetManagerId
) {
    public AssignInquiryManagerCommand toCommand(Long inquiryId, Long actorMemberId) {
        return new AssignInquiryManagerCommand(inquiryId, actorMemberId, targetManagerId);
    }
}
