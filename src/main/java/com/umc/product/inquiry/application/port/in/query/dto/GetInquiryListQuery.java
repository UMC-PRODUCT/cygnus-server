package com.umc.product.inquiry.application.port.in.query.dto;

import com.umc.product.inquiry.domain.enums.InquiryCategory;
import com.umc.product.inquiry.domain.enums.InquiryStatus;
import com.umc.product.inquiry.domain.enums.InquiryTarget;

public record GetInquiryListQuery(
    Long memberId,
    Long cursorId,
    int size,
    InquiryStatus status,       // null = 필터 미적용
    InquiryTarget target,       // null = 필터 미적용
    InquiryCategory category    // null = 필터 미적용
) {
}
