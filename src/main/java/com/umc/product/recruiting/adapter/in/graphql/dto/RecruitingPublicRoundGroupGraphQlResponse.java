package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicRoundGroupInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicRoundInfo;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public record RecruitingPublicRoundGroupGraphQlResponse(
    String seasonId,
    String gisuId,
    String chapterId,
    String chapterName,
    String schoolId,
    String schoolName,
    List<Round> rounds
) {

    public static RecruitingPublicRoundGroupGraphQlResponse from(RecruitingPublicRoundGroupInfo info) {
        return new RecruitingPublicRoundGroupGraphQlResponse(
            GlobalId.encode(GlobalIdTypes.RECRUITING_SEASON, info.seasonId()),
            GlobalId.encode(GlobalIdTypes.GISU, info.gisuId()),
            GlobalId.encode(GlobalIdTypes.CHAPTER, info.chapterId()),
            info.chapterName(),
            GlobalId.encode(GlobalIdTypes.SCHOOL, info.schoolId()),
            info.schoolName(),
            info.rounds().stream().map(Round::from).toList()
        );
    }

    public record Round(
        String roundId,
        String title,
        RecruitingRoundType type,
        Integer roundNo,
        List<ChallengerTrack> recruitableTracks,
        boolean secondChoiceEnabled,
        Instant documentStartAt,
        Instant documentEndAt,
        Instant documentResultPublishedAt,
        boolean interviewRequired,
        Instant interviewStartAt,
        Instant interviewEndAt,
        Instant finalResultPublishedAt,
        String announcement,
        String applicationFormId,
        String formId,
        boolean applicationOpen
    ) {

        private static Round from(RecruitingPublicRoundInfo info) {
            return new Round(
                GlobalId.encode(GlobalIdTypes.RECRUITING_ROUND, info.roundId()),
                info.title(),
                info.type(),
                info.roundNo(),
                info.recruitableTracks(),
                info.secondChoiceEnabled(),
                info.documentStartAt(),
                info.documentEndAt(),
                info.documentResultPublishedAt(),
                info.interviewRequired(),
                info.interviewStartAt(),
                info.interviewEndAt(),
                info.finalResultPublishedAt(),
                info.announcement(),
                GlobalId.encode(GlobalIdTypes.RECRUITING_APPLICATION_FORM, info.applicationFormId()),
                GlobalId.encode(GlobalIdTypes.FORM, info.formId()),
                info.applicationOpen()
            );
        }
    }
}
