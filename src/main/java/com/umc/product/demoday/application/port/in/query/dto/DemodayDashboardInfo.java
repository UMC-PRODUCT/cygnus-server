package com.umc.product.demoday.application.port.in.query.dto;

import java.time.Instant;
import java.util.List;

public record DemodayDashboardInfo(
        Long pollId,
        Instant generatedAt,
        SummaryInfo summary,
        List<RankingInfo> rankings,
        List<StampHeatmapInfo> stampHeatmap
) {

    public record SummaryInfo(
            int boothCount,
            int totalVoteCount
    ) {
    }

    public record RankingInfo(
            int rank,
            Long boothId,
            Integer boothCode,
            Long projectId,
            String displayName,
            int voteCount
    ) {
    }

    public record StampHeatmapInfo(
            Long boothId,
            Integer boothCode,
            Long projectId,
            String displayName,
            int stampCount
    ) {
    }
}
