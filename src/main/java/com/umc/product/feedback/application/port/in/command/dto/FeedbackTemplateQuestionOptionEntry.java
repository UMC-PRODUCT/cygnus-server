package com.umc.product.feedback.application.port.in.command.dto;

import java.util.Objects;

import lombok.Builder;

@Builder
public record FeedbackTemplateQuestionOptionEntry(
    Long optionId,
    String content,
    long orderNo,
    boolean isOther
) {
    public FeedbackTemplateQuestionOptionEntry {
        Objects.requireNonNull(content, "option content must not be null");
    }
}
