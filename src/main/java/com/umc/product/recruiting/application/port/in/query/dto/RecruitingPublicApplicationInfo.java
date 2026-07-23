package com.umc.product.recruiting.application.port.in.query.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.query.dto.AnswerInfo;
import com.umc.product.recruiting.domain.enums.RecruitingPublicResultStatus;

import lombok.Builder;

@Builder
public record RecruitingPublicApplicationInfo(
    Long applicationId,
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
    List<Answer> answers
) {

    @Builder
    public record Answer(
        Long questionId,
        String textValue,
        List<Long> selectedOptionIds,
        Set<String> fileIds,
        Set<Instant> times
    ) {

        public static Answer from(AnswerInfo info) {
            return Answer.builder()
                .questionId(info.questionId())
                .textValue(info.textValue())
                .selectedOptionIds(info.selectedOptions().stream()
                    .map(AnswerInfo.SelectedOption::questionOptionId)
                    .toList())
                .fileIds(info.fileIds())
                .times(info.times())
                .build();
        }
    }
}
