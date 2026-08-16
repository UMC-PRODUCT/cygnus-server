package com.umc.product.project.application.port.in.query.dto.statistics;

import java.util.List;

/**
 * 지부 단위 지원/매칭 요약 통계.
 */
public record ChapterProjectStatisticsSummaryInfo(
    List<RoundApplicationStatisticsInfo> roundApplicationStatistics,
    List<RoundSchoolApplicationStatisticsInfo> roundSchoolRankings,
    List<SchoolApplicationMatchingStatisticsInfo> schoolMatchingStatistics,
    List<ProjectRoundMemberStatisticsInfo> projectRoundStatistics
) {
}
