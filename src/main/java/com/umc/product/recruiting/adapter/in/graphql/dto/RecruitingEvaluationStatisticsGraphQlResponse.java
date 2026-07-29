package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingChapterEvaluationStatisticsInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingEvaluationStatisticsInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSchoolEvaluationStatisticsInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingTrackEvaluationCountInfo;

public record RecruitingEvaluationStatisticsGraphQlResponse(
    Instant asOf,
    Long applicantCount,
    Long evaluatedCount,
    List<TrackCount> byTrack,
    List<ChapterStatistics> chapters
) {

    public static RecruitingEvaluationStatisticsGraphQlResponse from(RecruitingEvaluationStatisticsInfo info) {
        return new RecruitingEvaluationStatisticsGraphQlResponse(
            info.asOf(),
            info.applicantCount(),
            info.evaluatedCount(),
            info.byTrack().stream().map(TrackCount::from).toList(),
            info.chapters().stream().map(ChapterStatistics::from).toList()
        );
    }

    public record TrackCount(
        ChallengerTrack track,
        Long applicantCount,
        Long evaluatedCount
    ) {

        private static TrackCount from(RecruitingTrackEvaluationCountInfo info) {
            return new TrackCount(info.track(), info.applicantCount(), info.evaluatedCount());
        }
    }

    public record ChapterStatistics(
        Long chapterId,
        String chapterName,
        Long applicantCount,
        Long evaluatedCount,
        List<TrackCount> byTrack,
        List<SchoolStatistics> schools
    ) {

        private static ChapterStatistics from(RecruitingChapterEvaluationStatisticsInfo info) {
            return new ChapterStatistics(
                info.chapterId(),
                info.chapterName(),
                info.applicantCount(),
                info.evaluatedCount(),
                info.byTrack().stream().map(TrackCount::from).toList(),
                info.schools().stream().map(SchoolStatistics::from).toList()
            );
        }
    }

    public record SchoolStatistics(
        Long schoolId,
        String schoolName,
        Long applicantCount,
        Long evaluatedCount,
        List<TrackCount> byTrack
    ) {

        private static SchoolStatistics from(RecruitingSchoolEvaluationStatisticsInfo info) {
            return new SchoolStatistics(
                info.schoolId(),
                info.schoolName(),
                info.applicantCount(),
                info.evaluatedCount(),
                info.byTrack().stream().map(TrackCount::from).toList()
            );
        }
    }
}
