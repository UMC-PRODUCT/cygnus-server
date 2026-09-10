package com.umc.product.analytics.application.port.in.query.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;

import lombok.Builder;

@Builder
public record AdminOperationsAttendancePartsInfo(
    List<PartAttendanceInfo> byPart
) {

    public static AdminOperationsAttendancePartsInfo from(List<PartAttendanceInfo> byPart) {
        return AdminOperationsAttendancePartsInfo.builder()
            .byPart(List.copyOf(byPart))
            .build();
    }

    @Builder
    public record PartAttendanceInfo(
        ChallengerPart part,
        long totalParticipantCount,
        long attendedCount,
        double attendanceRate
    ) {

        public static PartAttendanceInfo of(
            ChallengerPart part,
            long totalParticipantCount,
            long attendedCount
        ) {
            double rate = totalParticipantCount > 0
                ? (double) attendedCount / totalParticipantCount
                : 0.0;
            return PartAttendanceInfo.builder()
                .part(part)
                .totalParticipantCount(totalParticipantCount)
                .attendedCount(attendedCount)
                .attendanceRate(rate)
                .build();
        }
    }
}
