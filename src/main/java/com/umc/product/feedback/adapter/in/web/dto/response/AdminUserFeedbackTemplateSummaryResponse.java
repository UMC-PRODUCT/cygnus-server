package com.umc.product.feedback.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateSummaryInfo;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;

import lombok.Builder;

@Builder
public record AdminUserFeedbackTemplateSummaryResponse(
    Long templateId,
    UserFeedbackContext context,
    UserFeedbackTargetType targetType,
    boolean isActive,
    Long formId,
    String title,
    Instant createdAt,
    Instant updatedAt
) {
    public static AdminUserFeedbackTemplateSummaryResponse from(UserFeedbackTemplateSummaryInfo info) {
        return AdminUserFeedbackTemplateSummaryResponse.builder()
            .templateId(info.templateId())
            .context(info.context())
            .targetType(info.targetType())
            .isActive(info.isActive())
            .formId(info.formId())
            .title(info.title())
            .createdAt(info.createdAt())
            .updatedAt(info.updatedAt())
            .build();
    }
}
