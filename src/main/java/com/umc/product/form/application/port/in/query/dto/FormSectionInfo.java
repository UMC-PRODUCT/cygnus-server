package com.umc.product.form.application.port.in.query.dto;

import com.umc.product.form.domain.FormSection;
import java.time.Instant;
import lombok.Builder;

/**
 * FormSection 단건 조회 결과 DTO.
 */
@Builder
public record FormSectionInfo(
    Long sectionId,
    Long formId,
    String title,
    String description,
    Long orderNo,
    Instant createdAt,
    Instant updatedAt
) {

    public static FormSectionInfo from(FormSection section) {
        return FormSectionInfo.builder()
            .sectionId(section.getId())
            .formId(section.getForm().getId())
            .title(section.getTitle())
            .description(section.getDescription())
            .orderNo(section.getOrderNo())
            .createdAt(section.getCreatedAt())
            .updatedAt(section.getUpdatedAt())
            .build();
    }
}
