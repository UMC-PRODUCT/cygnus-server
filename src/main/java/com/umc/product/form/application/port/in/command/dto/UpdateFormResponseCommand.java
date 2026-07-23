package com.umc.product.form.application.port.in.command.dto;

import java.util.List;
import java.util.Set;

import lombok.Builder;

@Builder
public record UpdateFormResponseCommand(
    Long formId,
    Long respondentMemberId,
    List<AnswerCommand> answers,
    Set<Long> requiredQuestionIds,
    Set<Long> allowedQuestionIds
) {
}
