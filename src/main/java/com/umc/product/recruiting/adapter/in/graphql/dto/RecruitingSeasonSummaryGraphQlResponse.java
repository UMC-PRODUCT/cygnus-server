package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonSummaryInfo;

public record RecruitingSeasonSummaryGraphQlResponse(
    Long seasonId,
    Long gisuId,
    Long chapterId,
    String chapterName,
    Long schoolId,
    String schoolName,
    String memo,
    List<RecruitingSeasonConfigurationGraphQlResponse.Round> rounds
) {

    public static RecruitingSeasonSummaryGraphQlResponse from(RecruitingSeasonSummaryInfo info) {
        return new RecruitingSeasonSummaryGraphQlResponse(
            info.seasonId(),
            info.gisuId(),
            info.chapterId(),
            info.chapterName(),
            info.schoolId(),
            info.schoolName(),
            info.memo(),
            info.rounds().stream()
                .map(detail -> RecruitingSeasonConfigurationGraphQlResponse.Round.from(detail.configuration()))
                .toList()
        );
    }
}
