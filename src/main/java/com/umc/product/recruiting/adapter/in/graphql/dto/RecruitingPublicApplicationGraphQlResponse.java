package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicApplicationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingPublicResultStatus;

public record RecruitingPublicApplicationGraphQlResponse(
    String applicationId,
    String applicantName,
    String applicantEmail,
    ChallengerTrack firstChoice,
    ChallengerTrack secondChoice,
    boolean submitted,
    boolean cancelled,
    boolean editable,
    RecruitingPublicResultStatus documentResult,
    RecruitingPublicResultStatus finalResult,
    ChallengerTrack acceptedTrack,
    List<AnswerGraphQlResponse> answers
) {

    public static RecruitingPublicApplicationGraphQlResponse from(RecruitingPublicApplicationInfo info) {
        return new RecruitingPublicApplicationGraphQlResponse(
            GlobalId.encode(GlobalIdTypes.RECRUITING_APPLICATION, info.applicationId()),
            info.applicantName(),
            info.applicantEmail(),
            info.firstChoice(),
            info.secondChoice(),
            info.submitted(),
            info.cancelled(),
            info.editable(),
            info.documentResult(),
            info.finalResult(),
            info.acceptedTrack(),
            info.answers().stream().map(AnswerGraphQlResponse::from).toList()
        );
    }

    public record AnswerGraphQlResponse(
        String questionId,
        String textValue,
        List<String> selectedOptionIds,
        Set<String> fileIds,
        Set<Instant> times
    ) {

        private static AnswerGraphQlResponse from(RecruitingPublicApplicationInfo.Answer answer) {
            return new AnswerGraphQlResponse(
                GlobalId.encode(GlobalIdTypes.FORM_QUESTION, answer.questionId()),
                answer.textValue(),
                answer.selectedOptionIds().stream()
                    .map(optionId -> GlobalId.encode(GlobalIdTypes.FORM_OPTION, optionId))
                    .toList(),
                answer.fileIds(),
                answer.times()
            );
        }
    }
}
