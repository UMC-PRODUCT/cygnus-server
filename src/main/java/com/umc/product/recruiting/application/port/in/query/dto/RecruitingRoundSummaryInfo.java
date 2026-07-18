package com.umc.product.recruiting.application.port.in.query.dto;

import com.umc.product.recruiting.domain.RecruitingRound;

public record RecruitingRoundSummaryInfo(
    Long seasonId,
    Long gisuId,
    Long chapterId,
    String chapterName,
    Long schoolId,
    String schoolName,
    RecruitingRoundConfigurationInfo round
) {

    public static RecruitingRoundSummaryInfo of(
        RecruitingRound round,
        Long chapterId,
        String chapterName,
        String schoolName
    ) {
        return new RecruitingRoundSummaryInfo(
            round.getSeason().getId(),
            round.getSeason().getGisuId(),
            chapterId,
            chapterName,
            round.getSeason().getSchoolId(),
            schoolName,
            RecruitingRoundConfigurationInfo.from(round)
        );
    }
}
