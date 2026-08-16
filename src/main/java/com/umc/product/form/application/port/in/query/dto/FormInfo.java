package com.umc.product.form.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.enums.FormStatus;

import lombok.Builder;

/**
 * Form 단건 조회 결과 DTO.
 */
@Builder
public record FormInfo(
    Long id,
    Long createdMemberId,
    String title,
    String description,
    FormStatus status,
    boolean isAnonymous,
    boolean allowDuplicateResponses,
    Instant createdAt,
    Instant updatedAt
) {

    public static FormInfo from(Form form) {
        return FormInfo.builder()
            .id(form.getId())
            .createdMemberId(form.getCreatedMemberId())
            .title(form.getTitle())
            .description(form.getDescription())
            .status(form.getStatus())
            .isAnonymous(form.isAnonymous())
            .allowDuplicateResponses(form.isAllowDuplicateResponses())
            .createdAt(form.getCreatedAt())
            .updatedAt(form.getUpdatedAt())
            .build();
    }
}
