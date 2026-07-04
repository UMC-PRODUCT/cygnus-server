package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 이미 제출된(SUBMITTED) 응답을 삭제하는 Command. 연관 Answer / AnswerChoice 도 cascade 삭제.
 * <p>
 * DRAFT 상태 응답 삭제는 {@code DeleteDraftFormResponseCommand} 를 사용한다.
 * 권한 검증은 응답 작성자 본인 여부.
 */
@Builder
public record DeleteFormResponseCommand(
    Long formId,
    Long respondentMemberId
) {
}
