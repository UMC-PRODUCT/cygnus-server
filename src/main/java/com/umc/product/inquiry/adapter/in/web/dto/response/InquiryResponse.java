package com.umc.product.inquiry.adapter.in.web.dto.response;

import com.umc.product.inquiry.application.port.in.query.dto.InquiryInfo;
import com.umc.product.inquiry.domain.enums.InquiryStatus;
import com.umc.product.inquiry.domain.enums.InquiryTarget;

public record InquiryResponse(
    Long id,
    InquiryTarget target,
    InquiryStatus status,
    Long chatRoomId,
    Long targetSchoolId,
    Long targetChapterId,
    Long targetGisuId
) {
    public static InquiryResponse from(InquiryInfo info) {
        return new InquiryResponse(
            info.id(),
            info.target(),
            info.status(),
            info.chatRoomId(),
            info.targetSchoolId(),
            info.targetChapterId(),
            info.targetGisuId()
        );
    }
}
