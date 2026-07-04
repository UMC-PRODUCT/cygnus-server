package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record UpdateFormResponseCommand(
    Long formId,
    Long respondentMemberId,
    List<AnswerCommand> answers
) {
}
