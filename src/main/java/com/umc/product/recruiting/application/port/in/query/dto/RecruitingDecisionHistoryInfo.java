package com.umc.product.recruiting.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionResult;

import lombok.Builder;

/**
 * 평가 이력 목록의 한 행입니다. 지원자 정보와 판정 담당자 정보를 함께 담습니다.
 */
@Builder
public record RecruitingDecisionHistoryInfo(
    Long decisionHistoryId,
    Long applicationId,
    Instant decidedAt,
    RecruitingApplicationStatus decisionStatus,
    RecruitingDecisionResult result,
    ApplicantInfo applicant,
    DeciderInfo decider
) {

    @Builder
    public record ApplicantInfo(
        Long chapterId,
        String chapterName,
        Long schoolId,
        String schoolName,
        String name,
        ChallengerTrack firstChoice,
        ChallengerTrack secondChoice,
        ChallengerTrack acceptedTrack
    ) {
    }

    /**
     * 담당자의 직위는 판정 시점 스냅샷이며, 소속 학교는 직위가 학교 단위일 때 지원서의 학교로 유도합니다.
     * 이름·닉네임은 조회 시점의 member 정보라 탈퇴한 담당자는 null일 수 있습니다.
     */
    @Builder
    public record DeciderInfo(
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
}
