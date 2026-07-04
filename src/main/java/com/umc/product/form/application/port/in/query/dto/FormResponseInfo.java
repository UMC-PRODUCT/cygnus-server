package com.umc.product.form.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.enums.FormResponseStatus;

import lombok.Builder;

/**
 * FormResponse 단건 조회 결과 DTO.
 * <p>
 * {@code submittedAt} / {@code submittedIp} 는 SUBMITTED 상태에서만 의미 있고, DRAFT 면 null.
 * {@code lastSavedAt} 은 모든 상태에서 최종 저장 시각.
 */
@Builder
public record FormResponseInfo(
    Long id,
    Long formId,
    Long respondentMemberId,
    FormResponseStatus status,
    Instant submittedAt,
    String submittedIp,
    Instant lastSavedAt,
    Instant createdAt,
    Instant updatedAt
) {

    public static FormResponseInfo from(FormResponse formResponse) {
        return FormResponseInfo.builder()
            .id(formResponse.getId())
            .formId(formResponse.getForm().getId())
            .respondentMemberId(formResponse.getRespondentMemberId())
            .status(formResponse.getStatus())
            .submittedAt(formResponse.getSubmittedAt())
            .submittedIp(formResponse.getSubmittedIp())
            .lastSavedAt(formResponse.getLastSavedAt())
            .createdAt(formResponse.getCreatedAt())
            .updatedAt(formResponse.getUpdatedAt())
            .build();
    }
}
