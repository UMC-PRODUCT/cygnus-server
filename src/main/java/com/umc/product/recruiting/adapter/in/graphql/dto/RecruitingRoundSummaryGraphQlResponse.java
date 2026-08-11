package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundSummaryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public record RecruitingRoundSummaryGraphQlResponse(
    String seasonId,
    String gisuId,
    String chapterId,
    String chapterName,
    String schoolId,
    String schoolName,
    String roundId,
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
    String availabilityFormId,
    String availabilityScheduleQuestionId,
    String announcement,
    String contactText
) {

    public static RecruitingRoundSummaryGraphQlResponse from(RecruitingRoundSummaryInfo info) {
        RecruitingRoundConfigurationInfo round = info.round();
        return new RecruitingRoundSummaryGraphQlResponse(
            GlobalId.encode(GlobalIdTypes.RECRUITING_SEASON, info.seasonId()),
            GlobalId.encode(GlobalIdTypes.GISU, info.gisuId()),
            GlobalId.encode(GlobalIdTypes.CHAPTER, info.chapterId()),
            info.chapterName(),
            GlobalId.encode(GlobalIdTypes.SCHOOL, info.schoolId()),
            info.schoolName(),
            GlobalId.encode(GlobalIdTypes.RECRUITING_ROUND, round.id()),
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
            encodeNullable(GlobalIdTypes.FORM, round.availabilityFormId()),
            encodeNullable(GlobalIdTypes.FORM_QUESTION, round.availabilityScheduleQuestionId()),
            round.announcement(),
            round.contactText()
        );
    }

    private static String encodeNullable(String typeName, Long rawId) {
        return rawId == null ? null : GlobalId.encode(typeName, rawId);
    }
}
