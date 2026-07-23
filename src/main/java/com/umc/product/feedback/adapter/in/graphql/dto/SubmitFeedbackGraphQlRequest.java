package com.umc.product.feedback.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.feedback.application.port.in.command.dto.SubmitUserFeedbackResponseCommand;
import com.umc.product.form.application.port.in.command.dto.AnswerCommand;

public record SubmitFeedbackGraphQlRequest(
    Long templateId,
    List<Answer> answers
) {

    public SubmitUserFeedbackResponseCommand toCommand(Long respondentMemberId) {
        return SubmitUserFeedbackResponseCommand.builder()
            .templateId(templateId)
            .respondentMemberId(respondentMemberId)
            .answers(answers.stream().map(Answer::toCommand).toList())
            .build();
    }

    public record Answer(
        Long questionId,
        String textValue,
        List<Long> selectedOptionIds,
        List<String> fileIds
    ) {

        private AnswerCommand toCommand() {
            return AnswerCommand.builder()
                .questionId(questionId)
                .textValue(textValue)
                .selectedOptionIds(selectedOptionIds)
                .fileIds(fileIds)
                .build();
        }
    }
}
