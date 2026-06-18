package com.umc.product.feedback.application.port.in.command.dto;

import java.util.List;
import java.util.Objects;

import com.umc.product.form.domain.enums.QuestionType;

import lombok.Builder;

@Builder
public record FeedbackTemplateQuestionEntry(
    Long questionId,
    QuestionType type,
    String title,
    String description,
    boolean isRequired,
    long orderNo,
    List<FeedbackTemplateQuestionOptionEntry> options
) {
    public FeedbackTemplateQuestionEntry {
        Objects.requireNonNull(type, "question type must not be null");
        Objects.requireNonNull(title, "question title must not be null");
        Objects.requireNonNull(options, "question options must not be null");
    }
}
