package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonTrackQuotaInfo;

public record RecruitingSeasonGraphQlResponse(
    Long id,
    Long gisuId,
    Long schoolId,
    String memo,
    List<RecruitingSeasonTrackQuotaGraphQlResponse> quotas,
    List<RecruitingRoundGraphQlResponse> rounds
) {

    public static RecruitingSeasonGraphQlResponse from(RecruitingSeasonSummaryInfo info) {
        return new RecruitingSeasonGraphQlResponse(
            info.seasonId(),
            info.gisuId(),
            info.schoolId(),
            info.memo(),
            List.of(),
            info.rounds().stream()
                .map(round -> RecruitingRoundGraphQlResponse.from(round, info.seasonId(), info.gisuId(), info.schoolId()))
                .toList()
        );
    }

    public static RecruitingSeasonGraphQlResponse from(RecruitingSeasonConfigurationInfo info) {
        return new RecruitingSeasonGraphQlResponse(
            info.id(),
            info.gisuId(),
            info.schoolId(),
            info.memo(),
            info.quotas().stream().map(RecruitingSeasonTrackQuotaGraphQlResponse::from).toList(),
            info.rounds().stream()
                .map(round -> RecruitingRoundGraphQlResponse.from(round, info.id(), info.gisuId(), info.schoolId()))
                .toList()
        );
    }

    public static RecruitingSeasonGraphQlResponse reference(Long seasonId, Long gisuId, Long schoolId) {
        return new RecruitingSeasonGraphQlResponse(seasonId, gisuId, schoolId, null, List.of(), List.of());
    }

    public record RecruitingSeasonTrackQuotaGraphQlResponse(
        com.umc.product.common.domain.enums.ChallengerTrack track,
        int targetCount
    ) {

        private static RecruitingSeasonTrackQuotaGraphQlResponse from(RecruitingSeasonTrackQuotaInfo info) {
            return new RecruitingSeasonTrackQuotaGraphQlResponse(info.track(), info.targetCount());
        }
    }
}
