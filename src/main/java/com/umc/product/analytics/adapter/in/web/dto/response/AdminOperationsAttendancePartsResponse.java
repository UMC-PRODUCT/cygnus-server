package com.umc.product.analytics.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendancePartsInfo;
import com.umc.product.common.domain.enums.ChallengerPart;

import lombok.Builder;

@Builder
public record AdminOperationsAttendancePartsResponse(
    List<PartAttendanceResponse> byPart
) {

    public static AdminOperationsAttendancePartsResponse from(AdminOperationsAttendancePartsInfo info) {
        return AdminOperationsAttendancePartsResponse.builder()
            .byPart(info.byPart().stream()
                .map(PartAttendanceResponse::from)
                .toList())
            .build();
    }

    @Builder
    public record PartAttendanceResponse(
        ChallengerPart part,
        long totalParticipantCount,
        long attendedCount,
        double attendanceRate
    ) {

        public static PartAttendanceResponse from(AdminOperationsAttendancePartsInfo.PartAttendanceInfo info) {
            return PartAttendanceResponse.builder()
                .part(info.part())
                .totalParticipantCount(info.totalParticipantCount())
                .attendedCount(info.attendedCount())
                .attendanceRate(info.attendanceRate())
                .build();
        }
    }
}
