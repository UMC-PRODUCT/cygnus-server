package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;

public record RecruitingSeasonManagementGraphQlResponse(
    Long seasonId,
    Long gisuId,
    String memo,
    List<RecruitingSeasonGraphQlResponse.RecruitingSeasonTrackQuotaGraphQlResponse> quotas
) {

    public static RecruitingSeasonManagementGraphQlResponse from(RecruitingSeasonGraphQlResponse season) {
        return new RecruitingSeasonManagementGraphQlResponse(
            season.id(),
            season.gisuId(),
            season.memo(),
            season.quotas()
        );
    }
}
