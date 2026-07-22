package com.umc.product.recruiting.adapter.in.graphql.dto;

public record RecruitingRoundManagementGraphQlResponse(
    Long roundId,
    Long seasonId,
    Long availabilityFormId
) {

    public static RecruitingRoundManagementGraphQlResponse from(RecruitingRoundGraphQlResponse round) {
        return new RecruitingRoundManagementGraphQlResponse(
            round.id(),
            round.seasonId(),
            round.availabilityFormId()
        );
    }
}
