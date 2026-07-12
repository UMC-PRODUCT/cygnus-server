package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonTrackQuotaInfo;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;
import com.umc.product.recruiting.domain.enums.RecruitingSeasonStatus;

public record RecruitingSeasonConfigurationGraphQlResponse(
    Long id,
    Long gisuId,
    Long schoolId,
    RecruitingSeasonStatus status,
    List<TrackQuota> quotas,
    List<Round> rounds
) {

    public static RecruitingSeasonConfigurationGraphQlResponse from(RecruitingSeasonConfigurationInfo info) {
        return new RecruitingSeasonConfigurationGraphQlResponse(
            info.id(),
            info.gisuId(),
            info.schoolId(),
            info.status(),
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

        private static Round from(RecruitingRoundConfigurationInfo info) {
            return new Round(
                info.id(),
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
                info.availabilityFormId(),
                info.announcement(),
                info.contactText()
            );
        }
    }
}
