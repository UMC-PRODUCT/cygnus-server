package com.umc.product.recruiting.adapter.in.web.dto.request;

import java.util.List;

import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationDraftCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpdateRecruitingApplicationDraftRequest(
    @NotNull List<@Valid AnswerRequest> answers,
    Long requesterMemberId
) {

    public UpdateRecruitingApplicationDraftCommand toCommand(Long applicationId, Long resolvedRequesterMemberId) {
        return UpdateRecruitingApplicationDraftCommand.builder()
            .applicationId(applicationId)
            .requesterMemberId(resolvedRequesterMemberId)
            .answers(answers.stream()
                .map(AnswerRequest::toCommand)
                .toList())
            .build();
    }

    public record AnswerRequest(
        @NotNull Long questionId,
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
