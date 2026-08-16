package com.umc.product.recruiting.application.port.in.query.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public record RecruitingRoundConfigurationInfo(
    Long id,
    String title,
    RecruitingRoundType type,
    Integer roundNo,
    RecruitingRoundStatus status,
    List<ChallengerTrack> recruitableTracks,
    boolean secondChoiceEnabled,
    Instant documentStartAt,
    Instant documentEndAt,
    Instant documentResultPublishedAt,
    boolean interviewRequired,
    Instant interviewStartAt,
    Instant interviewEndAt,
    Instant finalResultPublishedAt,
    Long availabilityFormId,
    Long availabilityScheduleQuestionId,
    String announcement,
    String contactText
) {

    public static RecruitingRoundConfigurationInfo from(RecruitingRound round) {
        return new RecruitingRoundConfigurationInfo(
            round.getId(),
            round.getTitle(),
            round.getType(),
            round.getRoundNo(),
            round.getStatus(),
            List.copyOf(round.getRecruitableTracks()),
            round.isSecondChoiceEnabled(),
            round.getDocumentStartAt(),
            round.getDocumentEndAt(),
            round.getDocumentResultPublishedAt(),
            round.isInterviewRequired(),
            round.getInterviewStartAt(),
            round.getInterviewEndAt(),
            round.getFinalResultPublishedAt(),
            round.getAvailabilityFormId(),
            round.getAvailabilityScheduleQuestionId(),
            round.getAnnouncement(),
            round.getContactText()
        );
    }
}
