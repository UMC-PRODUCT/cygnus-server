package com.umc.product.recruiting.application.port.in.query.dto;

import java.util.List;

public record RecruitingSchoolEvaluationStatisticsInfo(
    Long schoolId,
    String schoolName,
    Long applicantCount,
    Long evaluatedCount,
    List<RecruitingTrackEvaluationCountInfo> byTrack
) {
}
