package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 개별 답변 삭제 Command. 연관 AnswerChoice 도 cascade 삭제.
 * DRAFT FormResponse 에 속한 답변만 삭제 가능.
 */
@Builder
public record DeleteAnswerCommand(
    Long answerId,
    Long requesterMemberId
) {
}
