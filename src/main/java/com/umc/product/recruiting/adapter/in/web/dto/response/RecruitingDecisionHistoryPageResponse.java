package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.global.response.PageResponse;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistoryPageInfo;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluationProgressStatus;

public record RecruitingDecisionHistoryPageResponse(
    Instant asOf,
    RecruitingEvaluationProgressStatus progressStatus,
    PageResponse<RecruitingDecisionHistoryResponse> histories
) {

    public static RecruitingDecisionHistoryPageResponse from(RecruitingDecisionHistoryPageInfo info) {
        return new RecruitingDecisionHistoryPageResponse(
            info.asOf(),
            info.progressStatus(),
            PageResponse.of(info.page(), RecruitingDecisionHistoryResponse::from)
        );
    }
}
