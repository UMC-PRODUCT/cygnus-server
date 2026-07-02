package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;

public record RecruitingInterviewScheduleCandidateGraphQlResponse(
    String startsAt,
    String endsAt,
    Integer availableApplicantCount
) {

    public static RecruitingInterviewScheduleCandidateGraphQlResponse from(
        Instant startsAt,
        Instant endsAt,
        Integer availableApplicantCount
    ) {
        return new RecruitingInterviewScheduleCandidateGraphQlResponse(
            startsAt == null ? null : startsAt.toString(),
            endsAt == null ? null : endsAt.toString(),
            availableApplicantCount
        );
    }
}
