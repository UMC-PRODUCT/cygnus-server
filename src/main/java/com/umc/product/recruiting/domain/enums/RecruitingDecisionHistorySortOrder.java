package com.umc.product.recruiting.domain.enums;

/**
 * 평가 이력 정렬 순서입니다. 담당자별 그룹 정렬 시에는 그룹 내부 정렬에 적용됩니다.
 */
public enum RecruitingDecisionHistorySortOrder {

    LATEST,
    OLDEST;

    public boolean isLatestFirst() {
        return this == LATEST;
    }
}
