package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingChapterEvaluationStatisticsInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingEvaluationStatisticsInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSchoolEvaluationStatisticsInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingTrackEvaluationCountInfo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "평가 현황 집계 응답")
public record RecruitingEvaluationStatisticsResponse(
    @Schema(description = "집계 기준 시각")
    Instant asOf,
    @Schema(description = "총 지원자 수 (DRAFT, CANCELLED 제외)", example = "1500")
    Long applicantCount,
    @Schema(description = "평가 완료 지원자 수 (서류 불합격 및 최종 판정 확정)", example = "1000")
    Long evaluatedCount,
    @Schema(description = "1지망 파트별 집계")
    List<TrackCountResponse> byTrack,
    @Schema(description = "지부별 집계 (지부 가나다순, 지부 내 학교 가나다순)")
    List<ChapterStatisticsResponse> chapters
) {

    public static RecruitingEvaluationStatisticsResponse from(RecruitingEvaluationStatisticsInfo info) {
        return new RecruitingEvaluationStatisticsResponse(
            info.asOf(),
            info.applicantCount(),
            info.evaluatedCount(),
            info.byTrack().stream().map(TrackCountResponse::from).toList(),
            info.chapters().stream().map(ChapterStatisticsResponse::from).toList()
        );
    }

    public record TrackCountResponse(
        ChallengerTrack track,
        Long applicantCount,
        Long evaluatedCount
    ) {

        private static TrackCountResponse from(RecruitingTrackEvaluationCountInfo info) {
            return new TrackCountResponse(info.track(), info.applicantCount(), info.evaluatedCount());
        }
    }

    public record ChapterStatisticsResponse(
        Long chapterId,
        String chapterName,
        Long applicantCount,
        Long evaluatedCount,
        List<TrackCountResponse> byTrack,
        List<SchoolStatisticsResponse> schools
    ) {

        private static ChapterStatisticsResponse from(RecruitingChapterEvaluationStatisticsInfo info) {
            return new ChapterStatisticsResponse(
                info.chapterId(),
                info.chapterName(),
                info.applicantCount(),
                info.evaluatedCount(),
                info.byTrack().stream().map(TrackCountResponse::from).toList(),
                info.schools().stream().map(SchoolStatisticsResponse::from).toList()
            );
        }
    }

    public record SchoolStatisticsResponse(
        Long schoolId,
        String schoolName,
        Long applicantCount,
        Long evaluatedCount,
        List<TrackCountResponse> byTrack
    ) {

        private static SchoolStatisticsResponse from(RecruitingSchoolEvaluationStatisticsInfo info) {
            return new SchoolStatisticsResponse(
                info.schoolId(),
                info.schoolName(),
                info.applicantCount(),
                info.evaluatedCount(),
                info.byTrack().stream().map(TrackCountResponse::from).toList()
            );
        }
    }
}
