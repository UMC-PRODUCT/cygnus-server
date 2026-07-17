package com.umc.product.feedback.adapter.in.graphql.dto;

import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateDetailInfo;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;

public record UserFeedbackTemplateGraphQlResponse(
    Long templateId,
    UserFeedbackContext context,
    UserFeedbackTargetType targetType,
    boolean active,
    UserFeedbackTemplateFormGraphQlResponse form,
    String createdAt,
    String updatedAt
) {

    public static UserFeedbackTemplateGraphQlResponse from(UserFeedbackTemplateDetailInfo info) {
        return new UserFeedbackTemplateGraphQlResponse(
            info.templateId(),
            info.context(),
            info.targetType(),
            info.isActive(),
            UserFeedbackTemplateFormGraphQlResponse.from(info.form()),
            info.createdAt().toString(),
            info.updatedAt().toString()
        );
    }
}
