package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicApplicationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingPublicResultStatus;

public record RecruitingApplicationPrivateGraphQlResponse(
    Long applicationId,
    String applicantName,
    String applicantEmail,
    boolean submitted,
    boolean cancelled,
    boolean editable,
    RecruitingPublicResultStatus documentResult,
    RecruitingPublicResultStatus finalResult,
    List<Answer> answers
) {

    public static RecruitingApplicationPrivateGraphQlResponse from(RecruitingPublicApplicationInfo info) {
        return new RecruitingApplicationPrivateGraphQlResponse(
            info.applicationId(),
            info.applicantName(),
            info.applicantEmail(),
            info.submitted(),
            info.cancelled(),
            info.editable(),
            info.documentResult(),
            info.finalResult(),
            info.answers().stream().map(Answer::from).toList()
        );
    }

    public record Answer(
        Long questionId,
        String textValue,
        List<Long> selectedOptionIds,
        Set<String> fileIds,
        Set<Instant> times
    ) {

        private static Answer from(RecruitingPublicApplicationInfo.Answer info) {
            return new Answer(
                info.questionId(),
                info.textValue(),
                info.selectedOptionIds(),
                info.fileIds(),
                info.times()
            );
        }
    }
}
