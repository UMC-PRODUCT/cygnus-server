package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 섹션 삭제 Command. 하위 질문/옵션 모두 cascade 삭제.
 */
@Builder
public record DeleteFormSectionCommand(
    Long sectionId,
    Long requesterMemberId
) {
}
