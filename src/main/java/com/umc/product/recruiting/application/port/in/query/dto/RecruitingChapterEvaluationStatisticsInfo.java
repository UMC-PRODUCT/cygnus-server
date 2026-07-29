package com.umc.product.recruiting.application.port.in.query.dto;

import java.util.List;

public record RecruitingChapterEvaluationStatisticsInfo(
    Long chapterId,
    String chapterName,
    Long applicantCount,
    Long evaluatedCount,
    List<RecruitingTrackEvaluationCountInfo> byTrack,
    List<RecruitingSchoolEvaluationStatisticsInfo> schools
) {
}
