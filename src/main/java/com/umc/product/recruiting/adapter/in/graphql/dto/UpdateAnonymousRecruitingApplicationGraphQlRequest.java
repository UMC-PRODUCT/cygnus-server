package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateAnonymousRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationDraftCommand;

public record UpdateAnonymousRecruitingApplicationGraphQlRequest(
    String credentialEmail,
    String applicationKey,
    String applicantName,
    String applicantEmail,
    ChallengerTrack firstChoice,
    ChallengerTrack secondChoice,
    List<AnswerGraphQlRequest> answers
) {

    public UpdateAnonymousRecruitingApplicationCommand toCommand() {
        List<UpdateRecruitingApplicationDraftCommand.AnswerEntry> answerEntries = answers == null
            ? List.of()
            : answers.stream().map(AnswerGraphQlRequest::toCommand).toList();
        return UpdateAnonymousRecruitingApplicationCommand.builder()
            .credentialEmail(credentialEmail)
            .applicationKey(applicationKey)
            .applicantName(applicantName)
            .applicantEmail(applicantEmail)
            .firstChoice(firstChoice)
            .secondChoice(secondChoice)
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
