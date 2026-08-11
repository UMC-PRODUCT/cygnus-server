package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
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
        String questionId,
        String textValue,
        List<String> selectedOptionIds,
        List<String> fileIds
    ) {

        private UpdateRecruitingApplicationDraftCommand.AnswerEntry toCommand() {
            return UpdateRecruitingApplicationDraftCommand.AnswerEntry.builder()
                .questionId(GlobalId.decodeLong(questionId, GlobalIdTypes.FORM_QUESTION))
                .textValue(textValue)
                .selectedOptionIds(selectedOptionIds == null
                    ? null
                    : selectedOptionIds.stream()
                        .map(optionId -> GlobalId.decodeLong(optionId, GlobalIdTypes.FORM_OPTION))
                        .toList())
                .fileIds(fileIds)
                .build();
        }
    }
}
