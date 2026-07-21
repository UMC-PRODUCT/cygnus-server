package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;
import java.util.Set;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryQuery;

public record RecruitingStatusSummaryGraphQlRequest(
    Long gisuId,
    List<Long> schoolIds,
    List<Long> roundIds,
    String schoolName
) {

    public RecruitingStatusSummaryGraphQlRequest {
        if (gisuId == null) {
            throw new IllegalArgumentException("gisuId는 필수입니다.");
        }
        validatePositive(schoolIds, "schoolIds");
        validatePositive(roundIds, "roundIds");
    }

    public RecruitingStatusSummaryQuery toQuery(Long requesterMemberId) {
        return RecruitingStatusSummaryQuery.builder()
            .gisuId(gisuId)
            .schoolIds(schoolIds == null ? Set.of() : Set.copyOf(schoolIds))
            .roundIds(roundIds == null ? Set.of() : Set.copyOf(roundIds))
            .schoolName(schoolName)
            .requesterMemberId(requesterMemberId)
            .build();
    }

    private static void validatePositive(List<Long> values, String fieldName) {
        if (values != null && values.stream().anyMatch(value -> value == null || value <= 0)) {
            throw new IllegalArgumentException(fieldName + "는 양수여야 합니다.");
        }
    }
}
