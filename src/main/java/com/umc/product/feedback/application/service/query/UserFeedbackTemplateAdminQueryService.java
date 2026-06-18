package com.umc.product.feedback.application.service.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.feedback.application.port.in.query.GetUserFeedbackTemplateAdminUseCase;
import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateDetailInfo;
import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateSummaryInfo;
import com.umc.product.feedback.application.port.out.LoadUserFeedbackTemplatePort;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.form.application.port.in.query.GetFormUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserFeedbackTemplateAdminQueryService implements GetUserFeedbackTemplateAdminUseCase {

    private final LoadUserFeedbackTemplatePort loadTemplatePort;
    private final GetFormUseCase getFormUseCase;

    @Override
    public List<UserFeedbackTemplateSummaryInfo> listTemplates(
        UserFeedbackContext context,
        UserFeedbackTargetType targetType,
        Boolean active
    ) {
        return loadTemplatePort.listByCondition(context, targetType, active).stream()
            .map(template -> UserFeedbackTemplateSummaryInfo.of(template, getFormUseCase.getById(template.getFormId())))
            .toList();
    }

    @Override
    public UserFeedbackTemplateDetailInfo getTemplate(Long templateId) {
        var template = loadTemplatePort.getById(templateId);
        return UserFeedbackTemplateDetailInfo.of(
            template,
            getFormUseCase.getFormWithStructure(template.getFormId())
        );
    }
}
