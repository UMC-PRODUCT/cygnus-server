package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

@Builder
public record CloseFormCommand(
    Long formId,
    Long requesterMemberId
) {
}
