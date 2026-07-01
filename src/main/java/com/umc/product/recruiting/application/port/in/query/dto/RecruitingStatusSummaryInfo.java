package com.umc.product.recruiting.application.port.in.query.dto;

import java.util.Map;

import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public record RecruitingStatusSummaryInfo(
    Long totalCount,
    Map<RecruitingApplicationStatus, Long> countByStatus
) {
}
