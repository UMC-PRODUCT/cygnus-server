package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 질문에 선택지를 추가하는 Command.
 * orderNo는 Service에서 자동 부여.
 * {@code isOther}는 '기타' 직접 입력 선택지 여부.
 * {@code nextSectionId}는 RADIO/DROPDOWN 타입에만 유효. null이면 다음 순서 섹션으로 이동.
 */
@Builder
public record CreateQuestionOptionCommand(
    Long questionId,
    String content,
    boolean isOther,
    Long nextSectionId
) {
}
