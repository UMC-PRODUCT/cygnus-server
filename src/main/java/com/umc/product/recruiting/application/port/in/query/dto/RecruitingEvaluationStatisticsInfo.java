package com.umc.product.recruiting.application.port.in.query.dto;

import java.time.Instant;
import java.util.List;

public record RecruitingEvaluationStatisticsInfo(
    Instant asOf,
    Long applicantCount,
    Long evaluatedCount,
    List<RecruitingTrackEvaluationCountInfo> byTrack,
    List<RecruitingChapterEvaluationStatisticsInfo> chapters
) {
}
