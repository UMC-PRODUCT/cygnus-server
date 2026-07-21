package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.util.List;
import java.util.Map;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지원 현황 상태별 집계 응답")
public record RecruitingStatusSummaryResponse(
    @Schema(description = "전체 지원서 수", example = "120")
    Long totalCount,
    @Schema(description = "지원서 상태별 지원서 수")
    Map<RecruitingApplicationStatus, Long> countByStatus,
    List<RoundSummaryResponse> rounds
) {

    public static RecruitingStatusSummaryResponse from(RecruitingStatusSummaryInfo info) {
        return new RecruitingStatusSummaryResponse(
            info.totalCount(),
            info.countByStatus(),
            info.rounds().stream().map(RoundSummaryResponse::from).toList()
        );
    }

    public record RoundSummaryResponse(
        Long roundId,
        RecruitingRoundType roundType,
        Integer roundNo,
        Long totalCount,
        Map<RecruitingApplicationStatus, Long> countByStatus
    ) {

        private static RoundSummaryResponse from(RecruitingRoundStatusSummaryInfo info) {
            return new RoundSummaryResponse(
                info.roundId(),
                info.roundType(),
                info.roundNo(),
                info.totalCount(),
                info.countByStatus()
            );
        }
    }
}
