package com.umc.product.inquiry.application.port.in.query.dto;

public record GetInquiryQuery(
    Long inquiryId,
    Long requesterMemberId
) {
}
