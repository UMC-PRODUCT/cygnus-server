package com.umc.product.inquiry.application.port.in.command.dto;

import com.umc.product.inquiry.domain.enums.InquiryCategory;
import com.umc.product.inquiry.domain.enums.InquiryTarget;

/**
 * 문의 생성 명령.
 * <p>
 * targetGisuId는 받지 않는다 — 서비스가 활성 기수를 자동 주입한다. authorMemberId는 컨트롤러에서 인증 주체로 채워 넘긴다. target이
 * SCHOOL/CHAPTER가 아닌 경우 targetSchoolId/targetChapterId는 무시되어 null로 저장된다.
 */
public record SubmitInquiryCommand(
    String title,
    String content,
    InquiryCategory category,
    InquiryTarget target,
    Long authorMemberId,
    Long targetSchoolId,
    Long targetChapterId
) {
}
