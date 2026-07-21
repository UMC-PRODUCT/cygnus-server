package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSchoolStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public record RecruitingStatusSummaryGraphQlResponse(
    Long totalCount,
    List<RecruitingStatusCountGraphQlResponse> countByStatus,
    List<SchoolSummary> schools
) {

    public static RecruitingStatusSummaryGraphQlResponse from(RecruitingStatusSummaryInfo info) {
        Map<RecruitingApplicationStatus, Long> countByStatus = info.countByStatus() == null
            ? Map.of()
            : info.countByStatus();
        return new RecruitingStatusSummaryGraphQlResponse(
            info.totalCount(),
            countByStatus.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().ordinal()))
                .map(entry -> new RecruitingStatusCountGraphQlResponse(entry.getKey(), entry.getValue()))
                .toList(),
            info.schools().stream().map(SchoolSummary::from).toList()
        );
    }

    public record SchoolSummary(
        Long schoolId,
        String schoolName,
        Long chapterId,
        String chapterName,
        Long totalCount,
        List<RecruitingStatusCountGraphQlResponse> countByStatus,
        List<RoundSummary> rounds
    ) {

        private static SchoolSummary from(RecruitingSchoolStatusSummaryInfo info) {
            return new SchoolSummary(
                info.schoolId(),
                info.schoolName(),
                info.chapterId(),
                info.chapterName(),
                info.totalCount(),
                toStatusCounts(info.countByStatus()),
                info.rounds().stream().map(RoundSummary::from).toList()
            );
        }
    }

    public record RoundSummary(
        Long roundId,
        String roundTitle,
        RecruitingRoundType roundType,
        Integer roundNo,
        Long totalCount,
        List<RecruitingStatusCountGraphQlResponse> countByStatus
    ) {

        private static RoundSummary from(RecruitingRoundStatusSummaryInfo info) {
            return new RoundSummary(
                info.roundId(),
                info.roundTitle(),
                info.roundType(),
                info.roundNo(),
                info.totalCount(),
                toStatusCounts(info.countByStatus())
            );
        }
    }

    private static List<RecruitingStatusCountGraphQlResponse> toStatusCounts(
        Map<RecruitingApplicationStatus, Long> counts
    ) {
        return counts.entrySet().stream()
            .sorted(Comparator.comparing(entry -> entry.getKey().ordinal()))
            .map(entry -> new RecruitingStatusCountGraphQlResponse(entry.getKey(), entry.getValue()))
            .toList();
    }
}
