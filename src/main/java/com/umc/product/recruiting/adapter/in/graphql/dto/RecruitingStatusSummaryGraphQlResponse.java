package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public record RecruitingStatusSummaryGraphQlResponse(
    Long totalCount,
    List<RecruitingStatusCountGraphQlResponse> countByStatus
) {

    public static RecruitingStatusSummaryGraphQlResponse from(RecruitingStatusSummaryInfo info) {
        Map<RecruitingApplicationStatus, Long> countByStatus = info.countByStatus() == null
            ? Map.of()
            : info.countByStatus();
        return new RecruitingStatusSummaryGraphQlResponse(
            info.totalCount(),
            countByStatus.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().ordinal()))
                .map(entry -> new RecruitingStatusCountGraphQlResponse(entry.getKey(), entry.getValue()))
                .toList()
        );
    }
}
