package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationDraftCommand;

public record UpdateRecruitingApplicationDraftGraphQlRequest(
    List<AnswerGraphQlRequest> answers,
    Long requesterMemberId
) {

    public UpdateRecruitingApplicationDraftCommand toCommand(Long applicationId, Long resolvedRequesterMemberId) {
        List<UpdateRecruitingApplicationDraftCommand.AnswerEntry> answerEntries = answers == null
            ? List.of()
            : answers.stream()
                .map(AnswerGraphQlRequest::toCommand)
                .toList();
        return UpdateRecruitingApplicationDraftCommand.builder()
            .applicationId(applicationId)
            .requesterMemberId(resolvedRequesterMemberId)
            .answers(answerEntries)
            .build();
    }

    public record AnswerGraphQlRequest(
        Long questionId,
        String textValue,
        List<Long> selectedOptionIds,
        List<String> fileIds
    ) {

        private UpdateRecruitingApplicationDraftCommand.AnswerEntry toCommand() {
            return UpdateRecruitingApplicationDraftCommand.AnswerEntry.builder()
                .questionId(questionId)
                .textValue(textValue)
                .selectedOptionIds(selectedOptionIds)
                .fileIds(fileIds)
                .build();
        }
    }
}
