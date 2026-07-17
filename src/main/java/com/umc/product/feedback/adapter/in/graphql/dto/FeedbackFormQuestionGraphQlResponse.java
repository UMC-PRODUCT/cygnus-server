package com.umc.product.feedback.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.QuestionType;

public record FeedbackFormQuestionGraphQlResponse(
    Long questionId,
    QuestionType type,
    String title,
    String description,
    boolean required,
    Long orderNo,
    List<FeedbackFormOptionGraphQlResponse> options
) {

    public static FeedbackFormQuestionGraphQlResponse from(FormWithStructureInfo.QuestionWithOptions question) {
        return new FeedbackFormQuestionGraphQlResponse(
            question.questionId(),
            question.type(),
            question.title(),
            question.description(),
            question.isRequired(),
            question.orderNo(),
            question.options().stream().map(FeedbackFormOptionGraphQlResponse::from).toList()
        );
    }
}
