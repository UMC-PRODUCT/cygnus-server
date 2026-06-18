package com.umc.product.feedback.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.feedback.domain.UserFeedbackTemplate;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;

import lombok.Builder;

@Builder
public record UserFeedbackTemplateDetailInfo(
    Long templateId,
    UserFeedbackContext context,
    UserFeedbackTargetType targetType,
    boolean isActive,
    FormWithStructureInfo form,
    Instant createdAt,
    Instant updatedAt
) {
    public static UserFeedbackTemplateDetailInfo of(UserFeedbackTemplate template, FormWithStructureInfo form) {
        return UserFeedbackTemplateDetailInfo.builder()
            .templateId(template.getId())
            .context(template.getContext())
            .targetType(template.getTargetType())
            .isActive(template.isActive())
            .form(form)
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .build();
    }
}
