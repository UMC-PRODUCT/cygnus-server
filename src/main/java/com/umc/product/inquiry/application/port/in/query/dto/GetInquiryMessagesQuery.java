package com.umc.product.inquiry.application.port.in.query.dto;

public record GetInquiryMessagesQuery(
    Long inquiryId,
    Long requesterMemberId,
    Long cursorId,
    int size
) {
}
