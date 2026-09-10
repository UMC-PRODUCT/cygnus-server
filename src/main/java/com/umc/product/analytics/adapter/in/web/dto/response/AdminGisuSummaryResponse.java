package com.umc.product.analytics.adapter.in.web.dto.response;

import com.umc.product.analytics.application.port.in.query.dto.AdminGisuSummaryInfo;
import lombok.Builder;

@Builder
public record AdminGisuSummaryResponse(
    long challengerCount,
    double graduationRate,
    double dropoutRate,
    double attendanceRate,
    double avgBonusPoint,
    double avgPenaltyPoint,
    long outCount
) {
    public static AdminGisuSummaryResponse from(AdminGisuSummaryInfo info) {
        return AdminGisuSummaryResponse.builder()
            .challengerCount(info.challengerCount())
            .graduationRate(info.graduationRate())
            .dropoutRate(info.dropoutRate())
            .attendanceRate(info.attendanceRate())
            .avgBonusPoint(info.avgBonusPoint())
            .avgPenaltyPoint(info.avgPenaltyPoint())
            .outCount(info.outCount())
            .build();
    }
}
