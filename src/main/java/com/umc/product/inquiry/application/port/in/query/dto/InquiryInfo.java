package com.umc.product.inquiry.application.port.in.query.dto;

import java.util.List;

import com.umc.product.inquiry.domain.Inquiry;
import com.umc.product.inquiry.domain.enums.InquiryCategory;
import com.umc.product.inquiry.domain.enums.InquiryStatus;
import com.umc.product.inquiry.domain.enums.InquiryTarget;

/**
 * 문의 조회/생성 결과 정보. 도메인 엔티티(Inquiry)를 외부 계층에 노출하지 않기 위한 전달용 record.
 */
public record InquiryInfo(
    Long id,
    String title,
    String content,
    InquiryCategory category,
    InquiryTarget target,
    InquiryStatus status,
    Long chatRoomId,
    Long authorMemberId,
    Long targetSchoolId,
    Long targetChapterId,
    Long targetGisuId,
    List<Long> assignedMemberIds,
    List<String> fileMetadataIds,
    boolean isRead,
    long unreadCount
) {
    public static InquiryInfo from(Inquiry inquiry, long unreadCount) {
        return new InquiryInfo(
            inquiry.getId(),
            inquiry.getTitle(),
            inquiry.getContent(),
            inquiry.getCategory(),
            inquiry.getTarget(),
            inquiry.getStatus(),
            inquiry.getChatRoomId(),
            inquiry.getAuthorMemberId(),
            inquiry.getTargetSchoolId(),
            inquiry.getTargetChapterId(),
            inquiry.getTargetGisuId(),
            inquiry.getAssignedMemberIds(),
            inquiry.getFileMetadataIds(),
            inquiry.isRead(),
            unreadCount
        );
    }
}
