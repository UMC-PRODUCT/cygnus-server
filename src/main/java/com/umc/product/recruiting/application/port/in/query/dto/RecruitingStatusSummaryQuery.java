package com.umc.product.recruiting.application.port.in.query.dto;

import java.util.Set;

import lombok.Builder;

@Builder
public record RecruitingStatusSummaryQuery(
    Long gisuId,
    Set<Long> schoolIds,
    Set<Long> roundIds,
    String schoolName,
    Long requesterMemberId
) {

    public RecruitingStatusSummaryQuery {
        schoolIds = schoolIds == null ? Set.of() : Set.copyOf(schoolIds);
        roundIds = roundIds == null ? Set.of() : Set.copyOf(roundIds);
        schoolName = schoolName == null || schoolName.isBlank() ? null : schoolName.trim();
    }
}
