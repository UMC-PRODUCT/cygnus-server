package com.umc.product.project.adapter.in.web.dto.response.statistics;

import java.util.List;

import com.umc.product.project.adapter.in.web.dto.response.statistics.ProjectStatisticsResponse.ProjectMatchingRoundStatisticsResponse;
import com.umc.product.project.adapter.in.web.dto.response.statistics.ProjectStatisticsResponse.RoundApplicationStatisticsResponse;
import com.umc.product.project.adapter.in.web.dto.response.statistics.ProjectStatisticsResponse.RoundSchoolApplicationStatisticsResponse;
import com.umc.product.project.application.port.in.query.dto.statistics.ChapterProjectStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ChapterProjectStatisticsSummaryInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ProjectMatchingRoundStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ProjectRoundMemberCountInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ProjectRoundMemberStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.SchoolApplicationMatchingStatisticsInfo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지부 전체 프로젝트 지원/매칭 현황 응답")
public record ChapterProjectStatisticsResponse(
    @Schema(description = "지부 ID")
    Long chapterId,
    @Schema(description = "지부 내 프로젝트별 기존 지원/매칭 현황")
    List<ProjectStatisticsResponse> projects,
    @Schema(description = "지부 단위 지원/매칭 요약")
    ChapterProjectStatisticsSummaryResponse summary
) {
    public static ChapterProjectStatisticsResponse from(ChapterProjectStatisticsInfo info) {
        return new ChapterProjectStatisticsResponse(
            info.chapterId(),
            info.projects().stream()
                .map(ProjectStatisticsResponse::from)
                .toList(),
            ChapterProjectStatisticsSummaryResponse.from(info.summary())
        );
    }

    @Schema(description = "지부 단위 지원/매칭 요약")
    public record ChapterProjectStatisticsSummaryResponse(
        @Schema(description = "매칭 차수별 지원 완료 인원 수와 지원 가능 총원")
        List<RoundApplicationStatisticsResponse> roundApplicationStatistics,
        @Schema(description = "매칭 차수별 지원자 학교 순위")
        List<RoundSchoolApplicationStatisticsResponse> roundSchoolRankings,
        @Schema(description = "학교별 지원 가능 총원, 지원 완료 인원 수, 매칭 완료 인원 수")
        List<SchoolMatchingStatisticsResponse> schoolMatchingStatistics,
        @Schema(description = "프로젝트별 매칭 차수 인원 수")
        List<ProjectRoundMemberStatisticsResponse> projectRoundStatistics
    ) {
        private static ChapterProjectStatisticsSummaryResponse from(ChapterProjectStatisticsSummaryInfo info) {
            return new ChapterProjectStatisticsSummaryResponse(
                info.roundApplicationStatistics().stream()
                    .map(RoundApplicationStatisticsResponse::from)
                    .toList(),
                info.roundSchoolRankings().stream()
                    .map(RoundSchoolApplicationStatisticsResponse::from)
                    .toList(),
                info.schoolMatchingStatistics().stream()
                    .map(SchoolMatchingStatisticsResponse::from)
                    .toList(),
                info.projectRoundStatistics().stream()
                    .map(ProjectRoundMemberStatisticsResponse::from)
                    .toList()
            );
        }
    }

    @Schema(description = "학교별 지원 가능 총원, 지원 완료 인원 수, 매칭 완료 인원 수")
    public record SchoolMatchingStatisticsResponse(
        @Schema(description = "학교 ID")
        Long schoolId,
        @Schema(description = "매칭 완료 인원 수")
        long matchedMemberCount,
        @Schema(description = "학교별 지원 가능 총원")
        long totalMemberCount,
        @Schema(description = "매칭 차수 중 한 번이라도 지원한 인원 수")
        long appliedMemberCount
    ) {
        private static SchoolMatchingStatisticsResponse from(SchoolApplicationMatchingStatisticsInfo info) {
            return new SchoolMatchingStatisticsResponse(
                info.schoolId(),
                info.matchedMemberCount(),
                info.totalMemberCount(),
                info.appliedMemberCount()
            );
        }
    }

    @Schema(description = "프로젝트별 매칭 차수 인원 수")
    public record ProjectRoundMemberStatisticsResponse(
        @Schema(description = "프로젝트 ID")
        Long projectId,
        @Schema(description = "매칭 차수별 인원 수")
        List<ProjectRoundMemberCountResponse> matchingRounds
    ) {
        private static ProjectRoundMemberStatisticsResponse from(ProjectRoundMemberStatisticsInfo info) {
            return new ProjectRoundMemberStatisticsResponse(
                info.projectId(),
                info.matchingRounds().stream()
                    .map(ProjectRoundMemberCountResponse::from)
                    .toList()
            );
        }
    }

    @Schema(description = "프로젝트 내 특정 매칭 차수의 지원 완료 인원 수와 매칭 완료 인원 수")
    public record ProjectRoundMemberCountResponse(
        @Schema(description = "매칭 차수")
        ProjectMatchingRoundStatisticsResponse matchingRound,
        @Schema(description = "지원 완료 인원 수")
        long appliedMemberCount,
        @Schema(description = "매칭 완료 인원 수")
        long matchedMemberCount
    ) {
        private static ProjectRoundMemberCountResponse from(ProjectRoundMemberCountInfo info) {
            return new ProjectRoundMemberCountResponse(
                toMatchingRoundResponse(info.matchingRound()),
                info.appliedMemberCount(),
                info.matchedMemberCount()
            );
        }
    }

    private static ProjectMatchingRoundStatisticsResponse toMatchingRoundResponse(
        ProjectMatchingRoundStatisticsInfo info
    ) {
        return new ProjectMatchingRoundStatisticsResponse(
            info.matchingRoundId(),
            info.type(),
            info.phase()
        );
    }
}
