package com.umc.product.form.application.port.in.command.dto;

import java.util.List;

import lombok.Builder;

/**
 * 폼 내 섹션 순서 재배치 Command.
 * {@code orderedSectionIds}의 순서대로 각 섹션의 orderNo가 1부터 재부여된다.
 * 해당 폼의 모든 섹션 ID가 포함되어야 한다.
 */
@Builder
public record ReorderFormSectionsCommand(
    Long formId,
    List<Long> orderedSectionIds
) {
}
