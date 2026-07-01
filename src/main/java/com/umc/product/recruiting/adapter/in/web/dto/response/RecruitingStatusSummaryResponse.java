package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.util.Map;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public record RecruitingStatusSummaryResponse(
    Long totalCount,
    Map<RecruitingApplicationStatus, Long> countByStatus
) {

    public static RecruitingStatusSummaryResponse from(RecruitingStatusSummaryInfo info) {
        return new RecruitingStatusSummaryResponse(info.totalCount(), info.countByStatus());
    }
}
