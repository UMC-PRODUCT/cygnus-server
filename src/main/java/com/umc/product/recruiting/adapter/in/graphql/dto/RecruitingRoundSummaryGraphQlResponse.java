package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundSummaryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public record RecruitingRoundSummaryGraphQlResponse(
    Long seasonId,
    Long gisuId,
    Long chapterId,
    String chapterName,
    Long schoolId,
    String schoolName,
    Long id,
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
    String announcement,
    String contactText
) {

    public static RecruitingRoundSummaryGraphQlResponse from(RecruitingRoundSummaryInfo info) {
        RecruitingRoundConfigurationInfo round = info.round();
        return new RecruitingRoundSummaryGraphQlResponse(
            info.seasonId(),
            info.gisuId(),
            info.chapterId(),
            info.chapterName(),
            info.schoolId(),
            info.schoolName(),
            round.id(),
            round.type(),
            round.roundNo(),
            round.status(),
            round.recruitableTracks(),
            round.secondChoiceEnabled(),
            round.documentStartAt(),
            round.documentEndAt(),
            round.documentResultPublishedAt(),
            round.interviewRequired(),
            round.interviewStartAt(),
            round.interviewEndAt(),
            round.finalResultPublishedAt(),
            round.availabilityFormId(),
            round.announcement(),
            round.contactText()
        );
    }
}
