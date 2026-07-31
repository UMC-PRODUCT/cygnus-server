package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistoryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionResult;

public record RecruitingDecisionHistoryResponse(
    Long decisionHistoryId,
    Long applicationId,
    Instant decidedAt,
    RecruitingApplicationStatus decisionStatus,
    RecruitingDecisionResult result,
    ApplicantResponse applicant,
    DeciderResponse decider
) {

    public static RecruitingDecisionHistoryResponse from(RecruitingDecisionHistoryInfo info) {
        return new RecruitingDecisionHistoryResponse(
            info.decisionHistoryId(),
            info.applicationId(),
            info.decidedAt(),
            info.decisionStatus(),
            info.result(),
            ApplicantResponse.from(info.applicant()),
            DeciderResponse.from(info.decider())
        );
    }

    public record ApplicantResponse(
        Long chapterId,
        String chapterName,
        Long schoolId,
        String schoolName,
        String name,
        ChallengerTrack firstChoice,
        ChallengerTrack secondChoice,
        ChallengerTrack acceptedTrack
    ) {

        public static ApplicantResponse from(RecruitingDecisionHistoryInfo.ApplicantInfo info) {
            return new ApplicantResponse(
                info.chapterId(),
                info.chapterName(),
                info.schoolId(),
                info.schoolName(),
                info.name(),
                info.firstChoice(),
                info.secondChoice(),
                info.acceptedTrack()
            );
        }
    }

    public record DeciderResponse(
        Long memberId,
        Long chapterId,
        String chapterName,
        Long schoolId,
        String schoolName,
        ChallengerRoleType roleType,
        String name,
        String nickname
    ) {

        public static DeciderResponse from(RecruitingDecisionHistoryInfo.DeciderInfo info) {
            return new DeciderResponse(
                info.memberId(),
                info.chapterId(),
                info.chapterName(),
                info.schoolId(),
                info.schoolName(),
                info.roleType(),
                info.name(),
                info.nickname()
            );
        }
    }
}
