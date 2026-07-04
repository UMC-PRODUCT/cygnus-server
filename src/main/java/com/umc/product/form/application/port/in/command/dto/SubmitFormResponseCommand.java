package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record SubmitFormResponseCommand(
    Long formId,
    Long respondentMemberId,
    List<AnswerCommand> answers
) {
}
