package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;

import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistoryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionResult;

public record RecruitingDecisionHistoryGraphQlResponse(
    Long decisionHistoryId,
    Long applicationId,
    Instant decidedAt,
    RecruitingApplicationStatus decisionStatus,
    RecruitingDecisionResult result,
    Applicant applicant,
    Decider decider
) {

    public static RecruitingDecisionHistoryGraphQlResponse from(RecruitingDecisionHistoryInfo info) {
        return new RecruitingDecisionHistoryGraphQlResponse(
            info.decisionHistoryId(),
            info.applicationId(),
            info.decidedAt(),
            info.decisionStatus(),
            info.result(),
            Applicant.from(info.applicant()),
            Decider.from(info.decider())
        );
    }

    public record Applicant(
        Long chapterId,
        String chapterName,
        Long schoolId,
        String schoolName,
        String name,
        ChallengerTrack firstChoice,
        ChallengerTrack secondChoice,
        ChallengerTrack acceptedTrack
    ) {

        public static Applicant from(RecruitingDecisionHistoryInfo.ApplicantInfo info) {
            return new Applicant(
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

    public record Decider(
        Long memberId,
        Long chapterId,
        String chapterName,
        Long schoolId,
        String schoolName,
        ChallengerRoleType roleType,
        String name,
        String nickname
    ) {

        public static Decider from(RecruitingDecisionHistoryInfo.DeciderInfo info) {
            return new Decider(
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
