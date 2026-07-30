package com.umc.product.recruiting.domain.enums;

import java.util.Set;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 평가 이력 화면의 평가 결과 필터입니다. 합격은 최종 합격, 불합격은 서류/최종 불합격을 뜻합니다.
 */
@Getter
@RequiredArgsConstructor
public enum RecruitingDecisionResult {

    PASSED(Set.of(RecruitingApplicationStatus.FINAL_PASSED)),
    FAILED(Set.of(RecruitingApplicationStatus.DOCUMENT_FAILED, RecruitingApplicationStatus.FINAL_FAILED));

    private final Set<RecruitingApplicationStatus> statuses;

    public static RecruitingDecisionResult from(RecruitingApplicationStatus status) {
        return status == RecruitingApplicationStatus.FINAL_PASSED ? PASSED : FAILED;
    }
}
