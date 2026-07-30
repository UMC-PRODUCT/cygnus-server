package com.umc.product.recruiting.application.port.in.query.dto;

import java.util.Map;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

/**
 * 1지망(firstChoice) 파트 기준 지원서 상태 교차집계.
 * <p>
 * DRAFT, CANCELLED 를 제외한 지원서를 파트별로 묶어 상태별 개수를 집계한다.
 * 파트별 합계는 각 계층의 totalCount 와 일치한다.
 */
public record RecruitingPartStatusSummaryInfo(
    ChallengerTrack part,
    Long totalCount,
    Map<RecruitingApplicationStatus, Long> countByStatus
) {
}
