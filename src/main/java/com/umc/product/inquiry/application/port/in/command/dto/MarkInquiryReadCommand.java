package com.umc.product.inquiry.application.port.in.command.dto;

public record MarkInquiryReadCommand(
    Long inquiryId,
    Long memberId
) {
    public static MarkInquiryReadCommand of(Long inquiryId, Long memberId) {
        return new MarkInquiryReadCommand(inquiryId, memberId);
    }
}
