package com.umc.product.feedback.adapter.in.graphql.dto;

import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateInfo;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.form.adapter.in.graphql.dto.FormGraphQlResponse;

public record UserFeedbackTemplateGraphQlResponse(
    Long templateId,
    UserFeedbackContext context,
    UserFeedbackTargetType targetType,
    FormGraphQlResponse form
) {

    public static UserFeedbackTemplateGraphQlResponse from(UserFeedbackTemplateInfo info) {
        return new UserFeedbackTemplateGraphQlResponse(
            info.templateId(),
            info.context(),
            info.targetType(),
            FormGraphQlResponse.from(info.form())
        );
    }
}
