package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 폼 삭제 Command. 연관된 섹션/질문/옵션/응답/답변 모두 함께 삭제됨.
 */
@Builder
public record DeleteFormCommand(
    Long formId,
    Long requesterMemberId
) {
}
