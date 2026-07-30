package com.umc.product.recruiting.application.port.out.dto;

import java.time.Instant;

import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

/**
 * 판정 이력 검색 결과의 평면 행입니다. 학교·지부 이름과 담당자 이름·닉네임은 서비스가 조회 시점에 결합합니다.
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
    ChallengerRoleType deciderRoleType
) {
}
