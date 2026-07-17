package com.umc.product.inquiry.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.inquiry.domain.Inquiry;
import com.umc.product.inquiry.domain.enums.InquiryCategory;
import com.umc.product.inquiry.domain.enums.InquiryStatus;
import com.umc.product.inquiry.domain.enums.InquiryTarget;

public record InquirySummaryInfo(
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
    public static InquirySummaryInfo from(Inquiry inquiry, long unreadCount) {
        return new InquirySummaryInfo(
            inquiry.getId(),
            inquiry.getTitle(),
            inquiry.getCategory(),
            inquiry.getTarget(),
            inquiry.getStatus(),
            inquiry.getAuthorMemberId(),
            inquiry.getTargetGisuId(),
            inquiry.getTargetSchoolId(),
            inquiry.getTargetChapterId(),
            inquiry.isRead(),
            unreadCount,
            inquiry.getCreatedAt()
        );
    }
}
