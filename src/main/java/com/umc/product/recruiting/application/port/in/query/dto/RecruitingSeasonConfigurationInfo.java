package com.umc.product.recruiting.application.port.in.query.dto;

import java.util.List;

import com.umc.product.recruiting.domain.RecruitingSeason;

public record RecruitingSeasonConfigurationInfo(
    Long id,
    Long gisuId,
    Long schoolId,
    String memo,
    Integer chapterTotalTargetCount,
    List<RecruitingSeasonTrackQuotaInfo> quotas,
    List<RecruitingRoundConfigurationInfo> rounds
) {

    public static RecruitingSeasonConfigurationInfo of(
        RecruitingSeason season,
        Integer chapterTotalTargetCount,
        List<RecruitingSeasonTrackQuotaInfo> quotas,
        List<RecruitingRoundConfigurationInfo> rounds
    ) {
        return new RecruitingSeasonConfigurationInfo(
            season.getId(),
            season.getGisuId(),
            season.getSchoolId(),
            season.getMemo(),
            chapterTotalTargetCount,
            List.copyOf(quotas),
            List.copyOf(rounds)
        );
    }
}
