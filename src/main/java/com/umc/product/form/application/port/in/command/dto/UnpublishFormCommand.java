package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

@Builder
public record UnpublishFormCommand(
    Long formId,
    Long requesterMemberId
) {
}
