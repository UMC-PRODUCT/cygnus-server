package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 선택지 삭제 Command.
 * 연관 AnswerChoice.question_option_id 는 SET NULL 처리됨.
 */
@Builder
public record DeleteQuestionOptionCommand(
    Long optionId,
    Long requesterMemberId
) {
}
