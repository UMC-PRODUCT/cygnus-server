package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicApplicationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingPublicResultStatus;

public record RecruitingPublicApplicationGraphQlResponse(
    Long applicationId,
    Long gisuId,
    Long roundId,
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
            info.applicationId(),
            info.gisuId(),
            info.roundId(),
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
        Long questionId,
        String textValue,
        List<Long> selectedOptionIds,
        Set<String> fileIds,
        Set<Instant> times
    ) {

        private static AnswerGraphQlResponse from(RecruitingPublicApplicationInfo.Answer answer) {
            return new AnswerGraphQlResponse(
                answer.questionId(),
                answer.textValue(),
                answer.selectedOptionIds(),
                answer.fileIds(),
                answer.times()
            );
        }
    }
}
