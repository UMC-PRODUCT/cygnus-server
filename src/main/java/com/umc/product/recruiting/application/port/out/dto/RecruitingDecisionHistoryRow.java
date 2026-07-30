package com.umc.product.recruiting.application.port.out.dto;

import java.time.Instant;

import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

/**
 * 판정 이력 검색 결과의 평면 행입니다. 담당자 정보는 판정 시점 스냅샷입니다.
 */
public record RecruitingDecisionHistoryRow(
    Long decisionHistoryId,
    Long applicationId,
    Long schoolId,
    String applicantName,
    String applicantEmail,
    ChallengerTrack firstChoice,
    ChallengerTrack secondChoice,
    ChallengerTrack acceptedTrack,
    RecruitingApplicationStatus decisionStatus,
    Instant decidedAt,
    Long decidedByMemberId,
    ChallengerRoleType deciderRoleType,
    Long deciderChapterId,
    String deciderChapterName,
    Long deciderSchoolId,
    String deciderSchoolName,
    String deciderName,
    String deciderNickname
) {
}
