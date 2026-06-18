package com.umc.product.feedback.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.List;

import com.umc.product.feedback.adapter.in.web.dto.common.FeedbackTemplateSectionItem;
import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateDetailInfo;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.FormStatus;

import lombok.Builder;

@Builder
public record AdminUserFeedbackTemplateResponse(
    Long templateId,
    UserFeedbackContext context,
    UserFeedbackTargetType targetType,
    boolean isActive,
    FeedbackTemplateForm form,
    Instant createdAt,
    Instant updatedAt
) {
    public static AdminUserFeedbackTemplateResponse from(UserFeedbackTemplateDetailInfo info) {
        return AdminUserFeedbackTemplateResponse.builder()
            .templateId(info.templateId())
            .context(info.context())
            .targetType(info.targetType())
            .isActive(info.isActive())
            .form(FeedbackTemplateForm.from(info.form()))
            .createdAt(info.createdAt())
            .updatedAt(info.updatedAt())
            .build();
    }

    @Builder
    public record FeedbackTemplateForm(
        Long formId,
        String title,
        String description,
        FormStatus status,
        boolean isAnonymous,
        boolean allowDuplicateResponses,
        List<FeedbackTemplateSectionItem> sections
    ) {
        public static FeedbackTemplateForm from(FormWithStructureInfo form) {
            return FeedbackTemplateForm.builder()
                .formId(form.formId())
                .title(form.title())
                .description(form.description())
                .status(form.status())
                .isAnonymous(form.isAnonymous())
                .allowDuplicateResponses(form.allowDuplicateResponses())
                .sections(form.sections().stream().map(FeedbackTemplateSectionItem::from).toList())
                .build();
        }
    }
}
