package com.umc.product.recruiting.application.port.in.query.dto;

import java.util.List;

import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingSeasonStatus;

public record RecruitingSeasonSummaryInfo(
    Long seasonId,
    Long gisuId,
    Long chapterId,
    String chapterName,
    Long schoolId,
    String schoolName,
    RecruitingSeasonStatus status,
    List<RecruitingRoundConfigurationInfo> rounds
) {

    public static RecruitingSeasonSummaryInfo of(
        RecruitingSeason season,
        Long chapterId,
        String chapterName,
        String schoolName,
        List<RecruitingRoundConfigurationInfo> rounds
    ) {
        return new RecruitingSeasonSummaryInfo(
            season.getId(),
            season.getGisuId(),
            chapterId,
            chapterName,
            season.getSchoolId(),
            schoolName,
            season.getStatus(),
            List.copyOf(rounds)
        );
    }
}
