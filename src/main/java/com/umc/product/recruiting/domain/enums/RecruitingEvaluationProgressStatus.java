package com.umc.product.recruiting.domain.enums;

/**
 * 평가 이력 헤더의 상태 뱃지입니다.
 * <p>
 * 판정 대상(DRAFT, CANCELLED 제외) 지원서를 분모로, 판정 완료(서류 불합격 또는 최종 판정) 지원서를 분자로 판단합니다.
 */
public enum RecruitingEvaluationProgressStatus {

    BEFORE_EVALUATION,
    IN_PROGRESS,
    COMPLETED
}
