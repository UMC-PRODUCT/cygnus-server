package com.umc.product.feedback.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;

public record UserFeedbackTemplateSectionGraphQlResponse(
    Long sectionId,
    String title,
    String description,
    Long orderNo,
    List<FeedbackFormQuestionGraphQlResponse> questions
) {

    public static UserFeedbackTemplateSectionGraphQlResponse from(
        FormWithStructureInfo.SectionWithQuestions section
    ) {
        return new UserFeedbackTemplateSectionGraphQlResponse(
            section.sectionId(),
            section.title(),
            section.description(),
            section.orderNo(),
            section.questions().stream().map(FeedbackFormQuestionGraphQlResponse::from).toList()
        );
    }
}
