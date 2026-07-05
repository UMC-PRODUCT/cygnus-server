package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 질문 삭제 Command. 하위 옵션/응답도 cascade 삭제.
 */
@Builder
public record DeleteQuestionCommand(
    Long questionId,
    Long requesterMemberId
) {
}
