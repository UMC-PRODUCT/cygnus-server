package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicRoundGroupInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicRoundInfo;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public record RecruitingPublicRoundGroupGraphQlResponse(
    Long seasonId,
    Long gisuId,
    Long chapterId,
    String chapterName,
    Long schoolId,
    String schoolName,
    List<Round> rounds
) {

    public static RecruitingPublicRoundGroupGraphQlResponse from(RecruitingPublicRoundGroupInfo info) {
        return new RecruitingPublicRoundGroupGraphQlResponse(
            info.seasonId(),
            info.gisuId(),
            info.chapterId(),
            info.chapterName(),
            info.schoolId(),
            info.schoolName(),
            info.rounds().stream().map(Round::from).toList()
        );
    }

    public record Round(
        Long roundId,
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
        Long applicationFormId,
        Long formId,
        boolean applicationOpen
    ) {

        private static Round from(RecruitingPublicRoundInfo info) {
            return new Round(
                info.roundId(),
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
                info.applicationFormId(),
                info.formId(),
                info.applicationOpen()
            );
        }
    }
}
