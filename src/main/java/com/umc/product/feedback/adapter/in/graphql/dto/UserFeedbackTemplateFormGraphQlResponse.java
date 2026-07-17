package com.umc.product.feedback.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.FormStatus;

public record UserFeedbackTemplateFormGraphQlResponse(
    Long formId,
    String title,
    String description,
    FormStatus status,
    boolean anonymous,
    boolean allowDuplicateResponses,
    List<UserFeedbackTemplateSectionGraphQlResponse> sections
) {

    public static UserFeedbackTemplateFormGraphQlResponse from(FormWithStructureInfo form) {
        return new UserFeedbackTemplateFormGraphQlResponse(
            form.formId(),
            form.title(),
            form.description(),
            form.status(),
            form.isAnonymous(),
            form.allowDuplicateResponses(),
            form.sections().stream().map(UserFeedbackTemplateSectionGraphQlResponse::from).toList()
        );
    }
}
