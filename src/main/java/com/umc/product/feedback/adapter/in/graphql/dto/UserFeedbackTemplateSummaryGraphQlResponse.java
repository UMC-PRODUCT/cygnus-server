package com.umc.product.feedback.adapter.in.graphql.dto;

import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateSummaryInfo;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;

public record UserFeedbackTemplateSummaryGraphQlResponse(
    Long templateId,
    UserFeedbackContext context,
    UserFeedbackTargetType targetType,
    boolean active,
    Long formId,
    String title,
    String createdAt,
    String updatedAt
) {

    public static UserFeedbackTemplateSummaryGraphQlResponse from(UserFeedbackTemplateSummaryInfo info) {
        return new UserFeedbackTemplateSummaryGraphQlResponse(
            info.templateId(),
            info.context(),
            info.targetType(),
            info.isActive(),
            info.formId(),
            info.title(),
            info.createdAt().toString(),
            info.updatedAt().toString()
        );
    }
}
