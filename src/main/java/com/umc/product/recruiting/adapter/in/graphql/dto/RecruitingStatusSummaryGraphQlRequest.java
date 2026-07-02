package com.umc.product.recruiting.adapter.in.graphql.dto;

public record RecruitingStatusSummaryGraphQlRequest(
    Long gisuId,
    Long schoolId
) {

    public RecruitingStatusSummaryGraphQlRequest {
        if (gisuId == null) {
            throw new IllegalArgumentException("gisuId는 필수입니다.");
        }
        if (schoolId == null) {
            throw new IllegalArgumentException("schoolId는 필수입니다.");
        }
    }
}
