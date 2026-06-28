package com.umc.product.analytics.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceChaptersInfo;

import lombok.Builder;

@Builder
public record AdminOperationsAttendanceChaptersResponse(
    List<ChapterAttendanceResponse> chapters
) {

    public static AdminOperationsAttendanceChaptersResponse from(AdminOperationsAttendanceChaptersInfo info) {
        return AdminOperationsAttendanceChaptersResponse.builder()
            .chapters(info.chapters().stream()
                .map(ChapterAttendanceResponse::from)
                .toList())
            .build();
    }

    @Builder
    public record ChapterAttendanceResponse(
        Long chapterId,
        String chapterName,
        long totalParticipantCount,
        long attendedCount,
        double attendanceRate,
        List<SchoolAttendanceResponse> schools
    ) {

        public static ChapterAttendanceResponse from(AdminOperationsAttendanceChaptersInfo.ChapterAttendanceInfo info) {
            return ChapterAttendanceResponse.builder()
                .chapterId(info.chapterId())
                .chapterName(info.chapterName())
                .totalParticipantCount(info.totalParticipantCount())
                .attendedCount(info.attendedCount())
                .attendanceRate(info.attendanceRate())
                .schools(info.schools().stream()
                    .map(SchoolAttendanceResponse::from)
                    .toList())
                .build();
        }
    }

    @Builder
    public record SchoolAttendanceResponse(
        Long schoolId,
        String schoolName,
        long totalParticipantCount,
        long attendedCount,
        double attendanceRate
    ) {

        public static SchoolAttendanceResponse from(AdminOperationsAttendanceChaptersInfo.SchoolAttendanceInfo info) {
            return SchoolAttendanceResponse.builder()
                .schoolId(info.schoolId())
                .schoolName(info.schoolName())
                .totalParticipantCount(info.totalParticipantCount())
                .attendedCount(info.attendedCount())
                .attendanceRate(info.attendanceRate())
                .build();
        }
    }
}
