package com.umc.product.feedback.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.feedback.domain.UserFeedbackTemplate;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.form.application.port.in.query.dto.FormInfo;

import lombok.Builder;

@Builder
public record UserFeedbackTemplateSummaryInfo(
    Long templateId,
    UserFeedbackContext context,
    UserFeedbackTargetType targetType,
    boolean isActive,
    Long formId,
    String title,
    Instant createdAt,
    Instant updatedAt
) {
    public static UserFeedbackTemplateSummaryInfo of(UserFeedbackTemplate template, FormInfo form) {
        return UserFeedbackTemplateSummaryInfo.builder()
            .templateId(template.getId())
            .context(template.getContext())
            .targetType(template.getTargetType())
            .isActive(template.isActive())
            .formId(template.getFormId())
            .title(form.title())
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .build();
    }
}
