package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.util.List;
import java.util.Map;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPartStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSchoolStatusSummaryInfo;
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
    @Schema(description = "1지망 파트별 상태 교차집계")
    List<PartSummaryResponse> parts,
    List<SchoolSummaryResponse> schools
) {

    public static RecruitingStatusSummaryResponse from(RecruitingStatusSummaryInfo info) {
        return new RecruitingStatusSummaryResponse(
            info.totalCount(),
            info.countByStatus(),
            info.parts().stream().map(PartSummaryResponse::from).toList(),
            info.schools().stream().map(SchoolSummaryResponse::from).toList()
        );
    }

    public record SchoolSummaryResponse(
        Long schoolId,
        String schoolName,
        Long chapterId,
        String chapterName,
        Long totalCount,
        Map<RecruitingApplicationStatus, Long> countByStatus,
        List<PartSummaryResponse> parts,
        List<RoundSummaryResponse> rounds
    ) {

        private static SchoolSummaryResponse from(RecruitingSchoolStatusSummaryInfo info) {
            return new SchoolSummaryResponse(
                info.schoolId(),
                info.schoolName(),
                info.chapterId(),
                info.chapterName(),
                info.totalCount(),
                info.countByStatus(),
                info.parts().stream().map(PartSummaryResponse::from).toList(),
                info.rounds().stream().map(RoundSummaryResponse::from).toList()
            );
        }
    }

    public record RoundSummaryResponse(
        Long roundId,
        String roundTitle,
        RecruitingRoundType roundType,
        Integer roundNo,
        Long totalCount,
        Map<RecruitingApplicationStatus, Long> countByStatus,
        List<PartSummaryResponse> parts
    ) {

        private static RoundSummaryResponse from(RecruitingRoundStatusSummaryInfo info) {
            return new RoundSummaryResponse(
                info.roundId(),
                info.roundTitle(),
                info.roundType(),
                info.roundNo(),
                info.totalCount(),
                info.countByStatus(),
                info.parts().stream().map(PartSummaryResponse::from).toList()
            );
        }
    }

    @Schema(description = "1지망 파트별 상태 교차집계 항목")
    public record PartSummaryResponse(
        @Schema(description = "1지망 파트")
        ChallengerTrack part,
        @Schema(description = "파트 지원서 수", example = "24")
        Long totalCount,
        @Schema(description = "파트 내 지원서 상태별 수")
        Map<RecruitingApplicationStatus, Long> countByStatus
    ) {

        private static PartSummaryResponse from(RecruitingPartStatusSummaryInfo info) {
            return new PartSummaryResponse(
                info.part(),
                info.totalCount(),
                info.countByStatus()
            );
        }
    }
}
