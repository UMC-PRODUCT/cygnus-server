package com.umc.product.inquiry.adapter.in.web.dto.request;

import com.umc.product.inquiry.application.port.in.command.dto.SubmitInquiryCommand;
import com.umc.product.inquiry.domain.enums.InquiryCategory;
import com.umc.product.inquiry.domain.enums.InquiryTarget;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 문의 생성 요청.
 * <p>
 * SCHOOL이면 targetSchoolId, CHAPTER이면 targetChapterId를 함께 보낸다(서비스에서 검증). gisu는 활성 기수가 자동 주입되므로 받지 않으며,
 * authorMemberId는 인증 주체에서 채워진다.
 */
public record SubmitInquiryRequest(
    @NotBlank(message = "제목은 필수입니다.") String title,

    @NotBlank(message = "내용은 필수입니다.") String content,

    @NotNull(message = "카테고리는 필수입니다.") InquiryCategory category,

    @NotNull(message = "문의 대상(target)은 필수입니다.")
    InquiryTarget target,

    Long targetSchoolId,
    Long targetChapterId
) {
    public SubmitInquiryCommand toCommand(Long authorMemberId) {
        return new SubmitInquiryCommand(
            title, content, category, target, authorMemberId, targetSchoolId, targetChapterId);
    }
}
