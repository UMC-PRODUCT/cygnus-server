package com.umc.product.analytics.application.port.in.query.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record AdminGisuPointsInfo(List<ChapterPointsInfo> chapters) {

    public static AdminGisuPointsInfo from(List<ChapterPointsInfo> chapters) {
        return AdminGisuPointsInfo.builder()
            .chapters(chapters)
            .build();
    }

    @Builder
    public record ChapterPointsInfo(
        Long chapterId,
        String chapterName,
        long challengerCount,
        double totalBonusPoint,
        double totalPenaltyPoint,
        double avgBonusPoint,
        double avgPenaltyPoint
    ) {
        public static ChapterPointsInfo of(
            Long chapterId,
            String chapterName,
            long challengerCount,
            double totalBonusPoint,
            double totalPenaltyPoint
        ) {
            double avgBonus = challengerCount > 0 ? totalBonusPoint / challengerCount : 0.0;
            double avgPenalty = challengerCount > 0 ? totalPenaltyPoint / challengerCount : 0.0;
            return ChapterPointsInfo.builder()
                .chapterId(chapterId)
                .chapterName(chapterName)
                .challengerCount(challengerCount)
                .totalBonusPoint(totalBonusPoint)
                .totalPenaltyPoint(totalPenaltyPoint)
                .avgBonusPoint(avgBonus)
                .avgPenaltyPoint(avgPenalty)
                .build();
        }
    }
}
