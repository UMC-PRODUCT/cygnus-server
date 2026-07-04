package com.umc.product.form.application.port.in.query.dto;

import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.enums.QuestionType;
import java.time.Instant;
import lombok.Builder;

/**
 * Question 단건 조회 결과 DTO.
 */
@Builder
public record QuestionInfo(
    Long id,
    Long sectionId,
    QuestionType type,
    String title,
    String description,
    boolean isRequired,
    Long orderNo,
    Instant createdAt,
    Instant updatedAt
) {

    public static QuestionInfo from(Question question) {
        return QuestionInfo.builder()
            .id(question.getId())
            .sectionId(question.getFormSection().getId())
            .type(question.getType())
            .title(question.getTitle())
            .description(question.getDescription())
            .isRequired(Boolean.TRUE.equals(question.getIsRequired()))
            .orderNo(question.getOrderNo())
            .createdAt(question.getCreatedAt())
            .updatedAt(question.getUpdatedAt())
            .build();
    }
}
