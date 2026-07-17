package com.umc.product.feedback.adapter.in.graphql.dto;

import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;

public record UserFeedbackTemplateSearchGraphQlRequest(
    UserFeedbackContext context,
    UserFeedbackTargetType targetType,
    Boolean active
) {
}
