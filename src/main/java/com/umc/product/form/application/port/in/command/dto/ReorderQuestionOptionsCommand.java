package com.umc.product.form.application.port.in.command.dto;

import java.util.List;

import lombok.Builder;

/**
 * 질문 내 선택지 순서 재배치 Command.
 */
@Builder
public record ReorderQuestionOptionsCommand(
    Long questionId,
    Long requesterMemberId,
    List<Long> orderedOptionIds
) {
}
