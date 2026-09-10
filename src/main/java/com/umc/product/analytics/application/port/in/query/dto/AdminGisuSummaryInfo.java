package com.umc.product.analytics.application.port.in.query.dto;

import lombok.Builder;

@Builder
public record AdminGisuSummaryInfo(
    long challengerCount,
    double graduationRate,
    double dropoutRate,
    double attendanceRate,
    double avgBonusPoint,
    double avgPenaltyPoint,
    long outCount
) {
    public static AdminGisuSummaryInfo of(
        long challengerCount,
        long graduatedCount,
        long dropoutCount,
        long totalParticipantCount,
        long attendedCount,
        double totalBonusPoint,
        double totalPenaltyPoint,
        long outCount
    ) {
        double graduationRate = challengerCount > 0 ? (double) graduatedCount / challengerCount : 0.0;
        double dropoutRate = challengerCount > 0 ? (double) dropoutCount / challengerCount : 0.0;
        double attendanceRate = totalParticipantCount > 0 ? (double) attendedCount / totalParticipantCount : 0.0;
        double avgBonusPoint = challengerCount > 0 ? totalBonusPoint / challengerCount : 0.0;
        double avgPenaltyPoint = challengerCount > 0 ? totalPenaltyPoint / challengerCount : 0.0;
        return AdminGisuSummaryInfo.builder()
            .challengerCount(challengerCount)
            .graduationRate(graduationRate)
            .dropoutRate(dropoutRate)
            .attendanceRate(attendanceRate)
            .avgBonusPoint(avgBonusPoint)
            .avgPenaltyPoint(avgPenaltyPoint)
            .outCount(outCount)
            .build();
    }
}
