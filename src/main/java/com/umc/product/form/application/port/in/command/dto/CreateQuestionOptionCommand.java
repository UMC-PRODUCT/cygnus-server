package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 질문에 선택지를 추가하는 Command.
 * orderNo는 Service에서 자동 부여.
 * {@code isOther}는 '기타' 직접 입력 선택지 여부.
 */
@Builder
public record CreateQuestionOptionCommand(
    Long questionId,
    Long requesterMemberId,
    String content,
    boolean isOther
) {
}
