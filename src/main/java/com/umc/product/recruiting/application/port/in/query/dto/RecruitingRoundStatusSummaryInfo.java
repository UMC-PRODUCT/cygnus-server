package com.umc.product.recruiting.application.port.in.query.dto;

import java.util.Map;

import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public record RecruitingRoundStatusSummaryInfo(
    Long roundId,
    String roundTitle,
    RecruitingRoundType roundType,
    Integer roundNo,
    Long totalCount,
    Map<RecruitingApplicationStatus, Long> countByStatus
) {
}
