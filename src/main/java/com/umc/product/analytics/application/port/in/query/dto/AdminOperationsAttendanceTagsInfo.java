package com.umc.product.analytics.application.port.in.query.dto;

import java.util.List;

import com.umc.product.schedule.domain.enums.ScheduleTag;

import lombok.Builder;

@Builder
public record AdminOperationsAttendanceTagsInfo(
    List<TagAttendanceInfo> byTag
) {

    public static AdminOperationsAttendanceTagsInfo from(List<TagAttendanceInfo> byTag) {
        return AdminOperationsAttendanceTagsInfo.builder()
            .byTag(List.copyOf(byTag))
            .build();
    }

    @Builder
    public record TagAttendanceInfo(
        ScheduleTag tag,
        long scheduleCount,
        long totalParticipantCount,
        long attendedCount,
        double attendanceRate
    ) {

        public static TagAttendanceInfo of(
            ScheduleTag tag,
            long scheduleCount,
            long totalParticipantCount,
            long attendedCount
        ) {
            double rate = totalParticipantCount > 0
                ? (double) attendedCount / totalParticipantCount
                : 0.0;
            return TagAttendanceInfo.builder()
                .tag(tag)
                .scheduleCount(scheduleCount)
                .totalParticipantCount(totalParticipantCount)
                .attendedCount(attendedCount)
                .attendanceRate(rate)
                .build();
        }
    }
}
