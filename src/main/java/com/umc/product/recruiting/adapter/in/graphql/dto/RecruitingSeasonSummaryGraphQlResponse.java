package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonSummaryInfo;

public record RecruitingSeasonSummaryGraphQlResponse(
    String seasonId,
    String gisuId,
    String chapterId,
    String chapterName,
    String schoolId,
    String schoolName,
    String memo,
    List<RecruitingSeasonGraphQlResponse.Round> rounds
) {

    public static RecruitingSeasonSummaryGraphQlResponse from(RecruitingSeasonSummaryInfo info) {
        return new RecruitingSeasonSummaryGraphQlResponse(
            GlobalId.encode(GlobalIdTypes.RECRUITING_SEASON, info.seasonId()),
            GlobalId.encode(GlobalIdTypes.GISU, info.gisuId()),
            GlobalId.encode(GlobalIdTypes.CHAPTER, info.chapterId()),
            info.chapterName(),
            GlobalId.encode(GlobalIdTypes.SCHOOL, info.schoolId()),
            info.schoolName(),
            info.memo(),
            info.rounds().stream()
                .map(RecruitingSeasonGraphQlResponse.Round::from)
                .toList()
        );
    }
}
