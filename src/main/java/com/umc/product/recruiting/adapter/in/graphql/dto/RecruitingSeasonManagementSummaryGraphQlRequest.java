package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;
import java.util.Set;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryQuery;

public record RecruitingSeasonManagementSummaryGraphQlRequest(
    List<Long> schoolIds,
    List<Long> roundIds,
    String schoolName
) {

    public RecruitingStatusSummaryQuery toQuery(Long gisuId, Long requesterMemberId) {
        return RecruitingStatusSummaryQuery.builder()
            .gisuId(gisuId)
            .schoolIds(schoolIds == null ? Set.of() : Set.copyOf(schoolIds))
            .roundIds(roundIds == null ? Set.of() : Set.copyOf(roundIds))
            .schoolName(schoolName)
            .requesterMemberId(requesterMemberId)
            .build();
    }
}
