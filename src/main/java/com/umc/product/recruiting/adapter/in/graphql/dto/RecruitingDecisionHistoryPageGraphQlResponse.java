package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistoryPageInfo;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluationProgressStatus;

public record RecruitingDecisionHistoryPageGraphQlResponse(
    Instant asOf,
    RecruitingEvaluationProgressStatus progressStatus,
    List<RecruitingDecisionHistoryGraphQlResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {

    public static RecruitingDecisionHistoryPageGraphQlResponse from(RecruitingDecisionHistoryPageInfo info) {
        return new RecruitingDecisionHistoryPageGraphQlResponse(
            info.asOf(),
            info.progressStatus(),
            info.page().getContent().stream().map(RecruitingDecisionHistoryGraphQlResponse::from).toList(),
            info.page().getNumber(),
            info.page().getSize(),
            info.page().getTotalElements(),
            info.page().getTotalPages(),
            info.page().hasNext()
        );
    }
}
