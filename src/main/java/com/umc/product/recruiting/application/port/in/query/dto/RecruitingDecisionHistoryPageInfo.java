package com.umc.product.recruiting.application.port.in.query.dto;

import java.time.Instant;

import org.springframework.data.domain.Page;

import com.umc.product.recruiting.domain.enums.RecruitingEvaluationProgressStatus;

/**
 * 평가 이력 목록과 헤더 집계입니다.
 * <p>
 * 상태 뱃지는 목록 필터 중 구조 조건(기수·지부·학교)만 반영하며, 파트·결과·이름 검색과는 무관합니다.
 */
public record RecruitingDecisionHistoryPageInfo(
    Instant asOf,
    RecruitingEvaluationProgressStatus progressStatus,
    Page<RecruitingDecisionHistoryInfo> page
) {
}
