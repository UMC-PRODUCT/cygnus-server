package com.umc.product.recruiting.domain;

import com.umc.product.common.domain.enums.ChallengerRoleType;

import lombok.Builder;

/**
 * 판정 시점의 담당자 정보를 보존하는 불변 스냅샷입니다.
 */
@Builder
public record RecruitingDecisionHistoryDeciderSnapshot(
    Long memberId,
    Long chapterId,
    String chapterName,
    Long schoolId,
    String schoolName,
    ChallengerRoleType roleType,
    String name,
    String nickname
) {
}
