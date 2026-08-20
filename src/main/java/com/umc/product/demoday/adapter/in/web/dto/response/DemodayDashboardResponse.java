package com.umc.product.demoday.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.List;

import com.umc.product.demoday.application.port.in.query.dto.DemodayDashboardInfo;

import io.swagger.v3.oas.annotations.media.Schema;

public record DemodayDashboardResponse(
        @Schema(description = "투표 ID", example = "101")
        Long pollId,
        @Schema(description = "서버가 통계 스냅샷을 생성한 시각", example = "2026-08-14T15:30:00+09:00")
        Instant generatedAt,
        @Schema(description = "요약 지표")
        SummaryResponse summary,
        @Schema(description = "득표 순으로 정렬된 프로젝트 부스 목록. 스탬프 적립 전용 외부 부스는 제외됩니다.")
        List<RankingEntryResponse> rankings,
        @Schema(description = "스탬프 획득 수를 포함한 전체 부스 목록")
        List<StampHeatmapEntryResponse> stampHeatmap
) {

    public static DemodayDashboardResponse from(DemodayDashboardInfo info) {
        return new DemodayDashboardResponse(
                info.pollId(),
                info.generatedAt(),
                SummaryResponse.from(info.summary()),
                info.rankings().stream().map(RankingEntryResponse::from).toList(),
                info.stampHeatmap().stream().map(StampHeatmapEntryResponse::from).toList()
        );
    }

    public record SummaryResponse(
            @Schema(description = "해당 Poll에서 운영하는 전체 부스 수", example = "30")
            int boothCount,
            @Schema(description = "프로젝트 부스에 저장된 유효 투표 수. 무효 처리된 표와 외부 부스 표는 제외됩니다.", example = "275")
            int totalVoteCount
    ) {

        public static SummaryResponse from(DemodayDashboardInfo.SummaryInfo info) {
            return new SummaryResponse(info.boothCount(), info.totalVoteCount());
        }
    }

    public record RankingEntryResponse(
            @Schema(description = "서버가 계산한 표시 순위. 동점이면 같은 값을 공유합니다.", example = "1")
            int rank,
            @Schema(description = "부스 ID", example = "9")
            Long boothId,
            @Schema(description = "UPMS에 등록된 프로젝트 ID", example = "509")
            Long projectId,
            @Schema(description = "화면에 표시할 부스 이름", example = "잇픽")
            String displayName,
            @Schema(description = "해당 부스의 유효 득표 수. 무효 처리된 표는 제외됩니다.", example = "20")
            int voteCount
    ) {

        public static RankingEntryResponse from(DemodayDashboardInfo.RankingInfo info) {
            return new RankingEntryResponse(
                    info.rank(),
                    info.boothId(),
                    info.projectId(),
                    info.displayName(),
                    info.voteCount()
            );
        }
    }

    public record StampHeatmapEntryResponse(
            @Schema(description = "부스 ID", example = "4")
            Long boothId,
            @Schema(description = "UPMS에 등록된 프로젝트 부스만 값을 가집니다. 외부 부스는 null입니다.", example = "504")
            Long projectId,
            @Schema(description = "화면에 표시할 부스 이름", example = "모디")
            String displayName,
            @Schema(description = "해당 부스에서 유효하게 적립된 스탬프 수", example = "123")
            int stampCount
    ) {

        public static StampHeatmapEntryResponse from(DemodayDashboardInfo.StampHeatmapInfo info) {
            return new StampHeatmapEntryResponse(
                    info.boothId(),
                    info.projectId(),
                    info.displayName(),
                    info.stampCount()
            );
        }
    }
}
