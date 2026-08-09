package com.umc.product.recruiting.application.port.in.query.dto;

import java.util.List;

import com.umc.product.recruiting.domain.RecruitingSeason;

public record RecruitingSeasonSummaryInfo(
    Long seasonId,
    Long gisuId,
    Long chapterId,
    String chapterName,
    Long schoolId,
    String schoolName,
    String memo,
    List<RecruitingRoundDetailInfo> rounds
) {

    public static RecruitingSeasonSummaryInfo of(
        RecruitingSeason season,
        Long chapterId,
        String chapterName,
        String schoolName,
        List<RecruitingRoundDetailInfo> rounds
    ) {
        return new RecruitingSeasonSummaryInfo(
            season.getId(),
            season.getGisuId(),
            chapterId,
            chapterName,
            season.getSchoolId(),
            schoolName,
            season.getMemo(),
            List.copyOf(rounds)
        );
    }
}
