package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 섹션의 title/description 업데이트 Command.
 * 순서 변경은 {@code ReorderFormSectionsCommand} 사용.
 */
@Builder
public record UpdateFormSectionCommand(
    Long sectionId,
    String title,
    String description,
    Boolean clearDescription
) {
}
