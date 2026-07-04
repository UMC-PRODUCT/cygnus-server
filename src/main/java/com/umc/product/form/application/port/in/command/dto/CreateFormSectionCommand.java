package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 폼에 섹션을 추가하는 Command.
 * orderNo는 Service에서 자동 부여하므로 포함하지 않음.
 */
@Builder
public record CreateFormSectionCommand(
    Long formId,
    Long requesterMemberId,
    String title,
    String description
) {
}
