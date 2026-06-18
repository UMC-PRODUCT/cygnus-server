package com.umc.product.feedback.application.port.in.command.dto;

import java.util.List;
import java.util.Objects;

import lombok.Builder;

@Builder
public record FeedbackTemplateSectionEntry(
    Long sectionId,
    String title,
    String description,
    long orderNo,
    List<FeedbackTemplateQuestionEntry> questions
) {
    public FeedbackTemplateSectionEntry {
        Objects.requireNonNull(title, "section title must not be null");
        Objects.requireNonNull(questions, "section questions must not be null");
    }
}
