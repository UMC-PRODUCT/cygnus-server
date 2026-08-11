package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.RelayNode;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonTrackQuotaInfo;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public record RecruitingSeasonGraphQlResponse(
    String id,
    String gisuId,
    String schoolId,
    String memo,
    List<TrackQuota> quotas,
    List<Round> rounds
) implements RelayNode {

    public static RecruitingSeasonGraphQlResponse from(RecruitingSeasonConfigurationInfo info) {
        return new RecruitingSeasonGraphQlResponse(
            GlobalId.encode(GlobalIdTypes.RECRUITING_SEASON, info.id()),
            GlobalId.encode(GlobalIdTypes.GISU, info.gisuId()),
            GlobalId.encode(GlobalIdTypes.SCHOOL, info.schoolId()),
            info.memo(),
            info.quotas().stream().map(TrackQuota::from).toList(),
            info.rounds().stream().map(Round::from).toList()
        );
    }

    public record TrackQuota(ChallengerTrack track, Integer targetCount) {

        private static TrackQuota from(RecruitingSeasonTrackQuotaInfo info) {
            return new TrackQuota(info.track(), info.targetCount());
        }
    }

    public record Round(
        String roundId,
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
        String availabilityFormId,
        String availabilityScheduleQuestionId,
        String announcement,
        String contactText
    ) {

        public static Round from(RecruitingRoundConfigurationInfo info) {
            return new Round(
                GlobalId.encode(GlobalIdTypes.RECRUITING_ROUND, info.id()),
                info.title(),
                info.type(),
                info.roundNo(),
                info.status(),
                info.recruitableTracks(),
                info.secondChoiceEnabled(),
                info.documentStartAt(),
                info.documentEndAt(),
                info.documentResultPublishedAt(),
                info.interviewRequired(),
                info.interviewStartAt(),
                info.interviewEndAt(),
                info.finalResultPublishedAt(),
                encodeNullable(GlobalIdTypes.FORM, info.availabilityFormId()),
                encodeNullable(GlobalIdTypes.FORM_QUESTION, info.availabilityScheduleQuestionId()),
                info.announcement(),
                info.contactText()
            );
        }

        private static String encodeNullable(String typeName, Long rawId) {
            return rawId == null ? null : GlobalId.encode(typeName, rawId);
        }
    }
}
