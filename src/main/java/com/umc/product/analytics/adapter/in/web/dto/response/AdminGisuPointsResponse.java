package com.umc.product.analytics.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.analytics.application.port.in.query.dto.AdminGisuPointsInfo;

import lombok.Builder;

@Builder
public record AdminGisuPointsResponse(List<ChapterPointsResponse> chapters) {

    public static AdminGisuPointsResponse from(AdminGisuPointsInfo info) {
        return AdminGisuPointsResponse.builder()
            .chapters(info.chapters().stream()
                .map(ChapterPointsResponse::from)
                .toList())
            .build();
    }

    @Builder
    public record ChapterPointsResponse(
        Long chapterId,
        String chapterName,
        long challengerCount,
        double totalBonusPoint,
        double totalPenaltyPoint,
        double avgBonusPoint,
        double avgPenaltyPoint
    ) {
        public static ChapterPointsResponse from(AdminGisuPointsInfo.ChapterPointsInfo info) {
            return ChapterPointsResponse.builder()
                .chapterId(info.chapterId())
                .chapterName(info.chapterName())
                .challengerCount(info.challengerCount())
                .totalBonusPoint(info.totalBonusPoint())
                .totalPenaltyPoint(info.totalPenaltyPoint())
                .avgBonusPoint(info.avgBonusPoint())
                .avgPenaltyPoint(info.avgPenaltyPoint())
                .build();
        }
    }
}
