package com.umc.product.analytics.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceTagsInfo;
import com.umc.product.schedule.domain.enums.ScheduleTag;

import lombok.Builder;

@Builder
public record AdminOperationsAttendanceTagsResponse(
    List<TagAttendanceResponse> byTag
) {

    public static AdminOperationsAttendanceTagsResponse from(AdminOperationsAttendanceTagsInfo info) {
        return AdminOperationsAttendanceTagsResponse.builder()
            .byTag(info.byTag().stream()
                .map(TagAttendanceResponse::from)
                .toList())
            .build();
    }

    @Builder
    public record TagAttendanceResponse(
        ScheduleTag tag,
        long scheduleCount,
        long totalParticipantCount,
        long attendedCount,
        double attendanceRate
    ) {

        public static TagAttendanceResponse from(AdminOperationsAttendanceTagsInfo.TagAttendanceInfo info) {
            return TagAttendanceResponse.builder()
                .tag(info.tag())
                .scheduleCount(info.scheduleCount())
                .totalParticipantCount(info.totalParticipantCount())
                .attendedCount(info.attendedCount())
                .attendanceRate(info.attendanceRate())
                .build();
        }
    }
}
