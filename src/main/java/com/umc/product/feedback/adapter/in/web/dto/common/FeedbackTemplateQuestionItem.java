package com.umc.product.feedback.adapter.in.web.dto.common;

import java.util.List;

import com.umc.product.feedback.application.port.in.command.dto.FeedbackTemplateQuestionEntry;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.QuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

@Builder
public record FeedbackTemplateQuestionItem(
    Long questionId,

    @NotNull(message = "질문 타입은 필수입니다")
    QuestionType type,

    @NotBlank(message = "질문 제목은 필수입니다")
    String title,

    String description,

    boolean isRequired,

    @PositiveOrZero(message = "orderNo는 0 이상이어야 합니다")
    long orderNo,

    @NotNull(message = "옵션 리스트는 null 일 수 없습니다")
    @Valid
    List<FeedbackTemplateOptionItem> options
) {
    public FeedbackTemplateQuestionEntry toEntry() {
        return FeedbackTemplateQuestionEntry.builder()
            .questionId(questionId)
            .type(type)
            .title(title)
            .description(description)
            .isRequired(isRequired)
            .orderNo(orderNo)
            .options(options.stream().map(FeedbackTemplateOptionItem::toEntry).toList())
            .build();
    }

    public static FeedbackTemplateQuestionItem from(FormWithStructureInfo.QuestionWithOptions question) {
        return FeedbackTemplateQuestionItem.builder()
            .questionId(question.questionId())
            .type(question.type())
            .title(question.title())
            .description(question.description())
            .isRequired(question.isRequired())
            .orderNo(question.orderNo())
            .options(question.options().stream().map(FeedbackTemplateOptionItem::from).toList())
            .build();
    }
}
