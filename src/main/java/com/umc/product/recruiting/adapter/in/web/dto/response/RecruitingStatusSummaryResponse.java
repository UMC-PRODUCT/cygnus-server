package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.util.Map;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지원 현황 상태별 집계 응답")
public record RecruitingStatusSummaryResponse(
    @Schema(description = "전체 지원서 수", example = "120")
    Long totalCount,
    @Schema(description = "지원서 상태별 지원서 수")
    Map<RecruitingApplicationStatus, Long> countByStatus
) {

    public static RecruitingStatusSummaryResponse from(RecruitingStatusSummaryInfo info) {
        return new RecruitingStatusSummaryResponse(info.totalCount(), info.countByStatus());
    }
}
