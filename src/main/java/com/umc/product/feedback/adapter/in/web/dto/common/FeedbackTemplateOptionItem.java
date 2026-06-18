package com.umc.product.feedback.adapter.in.web.dto.common;

import com.umc.product.feedback.application.port.in.command.dto.FeedbackTemplateQuestionOptionEntry;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

@Builder
public record FeedbackTemplateOptionItem(
    Long optionId,

    @NotBlank(message = "선택지 내용은 필수입니다")
    String content,

    @PositiveOrZero(message = "orderNo는 0 이상이어야 합니다")
    long orderNo,

    boolean isOther
) {
    public FeedbackTemplateQuestionOptionEntry toEntry() {
        return FeedbackTemplateQuestionOptionEntry.builder()
            .optionId(optionId)
            .content(content)
            .orderNo(orderNo)
            .isOther(isOther)
            .build();
    }

    public static FeedbackTemplateOptionItem from(FormWithStructureInfo.Option option) {
        return FeedbackTemplateOptionItem.builder()
            .optionId(option.optionId())
            .content(option.content())
            .orderNo(option.orderNo())
            .isOther(option.isOther())
            .build();
    }
}
