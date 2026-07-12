package com.umc.product.recruiting.application.port.in.query.dto;

import java.util.List;

import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingSeasonStatus;

public record RecruitingSeasonConfigurationInfo(
    Long id,
    Long gisuId,
    Long schoolId,
    RecruitingSeasonStatus status,
    List<RecruitingSeasonTrackQuotaInfo> quotas,
    List<RecruitingRoundConfigurationInfo> rounds
) {

    public static RecruitingSeasonConfigurationInfo of(
        RecruitingSeason season,
        List<RecruitingSeasonTrackQuotaInfo> quotas,
        List<RecruitingRoundConfigurationInfo> rounds
    ) {
        return new RecruitingSeasonConfigurationInfo(
            season.getId(),
            season.getGisuId(),
            season.getSchoolId(),
            season.getStatus(),
            List.copyOf(quotas),
            List.copyOf(rounds)
        );
    }
}
