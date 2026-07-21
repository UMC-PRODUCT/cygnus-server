package com.umc.product.recruiting.application.port.in.query.dto;

import java.util.List;
import java.util.Map;

import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public record RecruitingSchoolStatusSummaryInfo(
    Long schoolId,
    String schoolName,
    Long chapterId,
    String chapterName,
    Long totalCount,
    Map<RecruitingApplicationStatus, Long> countByStatus,
    List<RecruitingRoundStatusSummaryInfo> rounds
) {
}
