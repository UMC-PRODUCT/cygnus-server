package com.umc.product.feedback.application.port.in.query;

import java.util.List;

import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateDetailInfo;
import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateSummaryInfo;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;

public interface GetUserFeedbackTemplateAdminUseCase {

    List<UserFeedbackTemplateSummaryInfo> listTemplates(
        UserFeedbackContext context,
        UserFeedbackTargetType targetType,
        Boolean active
    );

    UserFeedbackTemplateDetailInfo getTemplate(Long templateId);
}
