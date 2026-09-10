package com.umc.product.analytics.application.port.in.query.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record AdminOperationsAttendanceChaptersInfo(
    List<ChapterAttendanceInfo> chapters
) {

    public static AdminOperationsAttendanceChaptersInfo from(List<ChapterAttendanceInfo> chapters) {
        return AdminOperationsAttendanceChaptersInfo.builder()
            .chapters(List.copyOf(chapters))
            .build();
    }

    @Builder
    public record ChapterAttendanceInfo(
        Long chapterId,
        String chapterName,
        long totalParticipantCount,
        long attendedCount,
        double attendanceRate,
        List<SchoolAttendanceInfo> schools
    ) {

        public static ChapterAttendanceInfo of(
            Long chapterId,
            String chapterName,
            long totalParticipantCount,
            long attendedCount,
            List<SchoolAttendanceInfo> schools
        ) {
            double rate = totalParticipantCount > 0
                ? (double) attendedCount / totalParticipantCount
                : 0.0;
            return ChapterAttendanceInfo.builder()
                .chapterId(chapterId)
                .chapterName(chapterName)
                .totalParticipantCount(totalParticipantCount)
                .attendedCount(attendedCount)
                .attendanceRate(rate)
                .schools(List.copyOf(schools))
                .build();
        }
    }

    @Builder
    public record SchoolAttendanceInfo(
        Long schoolId,
        String schoolName,
        long totalParticipantCount,
        long attendedCount,
        double attendanceRate
    ) {

        public static SchoolAttendanceInfo of(
            Long schoolId,
            String schoolName,
            long totalParticipantCount,
            long attendedCount
        ) {
            double rate = totalParticipantCount > 0
                ? (double) attendedCount / totalParticipantCount
                : 0.0;
            return SchoolAttendanceInfo.builder()
                .schoolId(schoolId)
                .schoolName(schoolName)
                .totalParticipantCount(totalParticipantCount)
                .attendedCount(attendedCount)
                .attendanceRate(rate)
                .build();
        }
    }
}
