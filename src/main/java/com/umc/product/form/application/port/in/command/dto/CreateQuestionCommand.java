package com.umc.product.form.application.port.in.command.dto;

import com.umc.product.form.domain.enums.QuestionType;

import lombok.Builder;

/**
 * 섹션에 질문을 추가하는 Command.
 * orderNo는 Service에서 자동 부여.
 */
@Builder
public record CreateQuestionCommand(
    Long sectionId,
    QuestionType type,
    String title,
    String description,
    boolean isRequired
) {
}
