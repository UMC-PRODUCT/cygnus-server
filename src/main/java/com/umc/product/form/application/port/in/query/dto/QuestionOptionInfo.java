package com.umc.product.form.application.port.in.query.dto;

import com.umc.product.form.domain.QuestionOption;
import java.time.Instant;
import lombok.Builder;

/**
 * QuestionOption 단건 조회 결과 DTO.
 */
@Builder
public record QuestionOptionInfo(
    Long id,
    Long questionId,
    String content,
    Long orderNo,
    boolean isOther,
    Instant createdAt,
    Instant updatedAt
) {

    public static QuestionOptionInfo from(QuestionOption option) {
        return QuestionOptionInfo.builder()
            .id(option.getId())
            .questionId(option.getQuestion().getId())
            .content(option.getContent())
            .orderNo(option.getOrderNo())
            .isOther(option.isOther())
            .createdAt(option.getCreatedAt())
            .updatedAt(option.getUpdatedAt())
            .build();
    }
}
