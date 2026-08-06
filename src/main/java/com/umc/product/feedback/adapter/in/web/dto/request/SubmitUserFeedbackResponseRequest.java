package com.umc.product.feedback.adapter.in.web.dto.request;

import java.util.List;

import com.umc.product.feedback.application.port.in.command.dto.SubmitUserFeedbackResponseCommand;
import com.umc.product.form.application.port.in.command.dto.AnswerCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record SubmitUserFeedbackResponseRequest(

    @NotNull(message = "templateId는 필수입니다") Long templateId,

    @NotNull(message = "answers는 null일 수 없습니다") @NotEmpty(message = "하나 이상의 답변이 필요합니다") List<@Valid @NotNull(message = "답변 항목은 null일 수 없습니다") UserFeedbackAnswerItem> answers
) {

    public record UserFeedbackAnswerItem(

        @NotNull(message = "questionId는 필수입니다") Long questionId,

        @Schema(description = "SHORT_TEXT / LONG_TEXT 타입에서 사용")
        String textValue,

        @Schema(description = "RADIO / CHECKBOX / DROPDOWN 타입에서 사용")
        List<Long> selectedOptionIds,

        @Schema(description = "FILE 타입에서 사용")
        List<String> fileIds
    ) {
        public AnswerCommand toCommand() {
            return AnswerCommand.builder()
                .questionId(questionId)
                .textValue(textValue)
                .selectedOptionIds(selectedOptionIds)
                .fileIds(fileIds)
                .build();
        }
    }

    public SubmitUserFeedbackResponseCommand toCommand(Long respondentMemberId) {
        return SubmitUserFeedbackResponseCommand.builder()
            .templateId(templateId)
            .respondentMemberId(respondentMemberId)
            .answers(answers.stream().map(UserFeedbackAnswerItem::toCommand).toList())
            .build();
    }
}
