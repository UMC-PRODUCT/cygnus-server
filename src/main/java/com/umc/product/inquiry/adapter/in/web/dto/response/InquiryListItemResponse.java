package com.umc.product.inquiry.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.inquiry.application.port.in.query.dto.InquirySummaryInfo;
import com.umc.product.inquiry.domain.enums.InquiryCategory;
import com.umc.product.inquiry.domain.enums.InquiryStatus;
import com.umc.product.inquiry.domain.enums.InquiryTarget;

public record InquiryListItemResponse(
    Long id,
    String title,
    InquiryCategory category,
    InquiryTarget target,
    InquiryStatus status,
    Long authorMemberId,
    Long targetGisuId,
    Long targetSchoolId,
    Long targetChapterId,
    boolean isRead,
    long unreadCount,
    Instant createdAt
) {
    public static InquiryListItemResponse from(InquirySummaryInfo info) {
        return new InquiryListItemResponse(
            info.id(),
            info.title(),
            info.category(),
            info.target(),
            info.status(),
            info.authorMemberId(),
            info.targetGisuId(),
            info.targetSchoolId(),
            info.targetChapterId(),
            info.isRead(),
            info.unreadCount(),
            info.createdAt()
        );
    }
}
