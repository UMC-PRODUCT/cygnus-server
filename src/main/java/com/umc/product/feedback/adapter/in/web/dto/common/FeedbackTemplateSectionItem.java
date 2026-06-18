package com.umc.product.feedback.adapter.in.web.dto.common;

import java.util.List;

import com.umc.product.feedback.application.port.in.command.dto.FeedbackTemplateSectionEntry;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

@Builder
public record FeedbackTemplateSectionItem(
    Long sectionId,

    @NotBlank(message = "섹션 제목은 필수입니다")
    String title,

    String description,

    @PositiveOrZero(message = "orderNo는 0 이상이어야 합니다")
    long orderNo,

    @NotNull(message = "질문 리스트는 null 일 수 없습니다")
    @Valid
    List<FeedbackTemplateQuestionItem> questions
) {
    public FeedbackTemplateSectionEntry toEntry() {
        return FeedbackTemplateSectionEntry.builder()
            .sectionId(sectionId)
            .title(title)
            .description(description)
            .orderNo(orderNo)
            .questions(questions.stream().map(FeedbackTemplateQuestionItem::toEntry).toList())
            .build();
    }

    public static FeedbackTemplateSectionItem from(FormWithStructureInfo.SectionWithQuestions section) {
        return FeedbackTemplateSectionItem.builder()
            .sectionId(section.sectionId())
            .title(section.title())
            .description(section.description())
            .orderNo(section.orderNo())
            .questions(section.questions().stream().map(FeedbackTemplateQuestionItem::from).toList())
            .build();
    }
}
