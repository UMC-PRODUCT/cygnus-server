package com.umc.product.form.application.port.in.command.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record SubmitFormResponseCommand(
    Long formId,
    List<AnswerCommand> answers
) {
}
