package com.umc.product.recruiting.application.port.in.command.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record UpdateRecruitingApplicationDraftCommand(
    Long applicationId,
    Long requesterMemberId,
    List<AnswerEntry> answers
) {

    @Builder
    public record AnswerEntry(
        Long questionId,
        String textValue,
        List<Long> selectedOptionIds,
        List<String> fileIds
    ) {
    }
}
