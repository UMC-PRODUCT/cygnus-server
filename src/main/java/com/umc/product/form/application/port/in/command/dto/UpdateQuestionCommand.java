package com.umc.product.form.application.port.in.command.dto;

import com.umc.product.form.domain.enums.QuestionType;

import lombok.Builder;

/**
 * 질문 속성 부분 업데이트 Command.
 * null 인 필드는 기존 값 유지 (PATCH semantics).
 * type 변경 시 Service가 관련 QuestionOption / 응답 cascade 정리 책임.
 */
@Builder
public record UpdateQuestionCommand(
    Long questionId,
    QuestionType type,
    String title,
    String description,
    Boolean clearDescription,
    Boolean isRequired
) {
}
