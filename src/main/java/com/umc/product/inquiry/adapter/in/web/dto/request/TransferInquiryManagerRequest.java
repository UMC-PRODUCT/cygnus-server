package com.umc.product.inquiry.adapter.in.web.dto.request;

import com.umc.product.inquiry.application.port.in.command.dto.TransferInquiryManagerCommand;

import jakarta.validation.constraints.NotNull;

public record TransferInquiryManagerRequest(
    @NotNull(message = "기존 담당자(fromManagerId)는 필수입니다.")
    Long fromManagerId,

    @NotNull(message = "새 담당자(toManagerId)는 필수입니다.")
    Long toManagerId
) {
    public TransferInquiryManagerCommand toCommand(Long inquiryId, Long actorMemberId) {
        return new TransferInquiryManagerCommand(inquiryId, actorMemberId, fromManagerId, toManagerId);
    }
}
