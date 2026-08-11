package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;

import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistoryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionResult;

public record RecruitingDecisionHistoryGraphQlResponse(
    String decisionHistoryId,
    String applicationId,
    Instant decidedAt,
    RecruitingApplicationStatus decisionStatus,
    RecruitingDecisionResult result,
    Applicant applicant,
    Decider decider
) {

    public static RecruitingDecisionHistoryGraphQlResponse from(RecruitingDecisionHistoryInfo info) {
        return new RecruitingDecisionHistoryGraphQlResponse(
            GlobalId.encode(GlobalIdTypes.RECRUITING_DECISION_HISTORY, info.decisionHistoryId()),
            GlobalId.encode(GlobalIdTypes.RECRUITING_APPLICATION, info.applicationId()),
            info.decidedAt(),
            info.decisionStatus(),
            info.result(),
            Applicant.from(info.applicant()),
            Decider.from(info.decider())
        );
    }

    public record Applicant(
        String chapterId,
        String chapterName,
        String schoolId,
        String schoolName,
        String name,
        ChallengerTrack firstChoice,
        ChallengerTrack secondChoice,
        ChallengerTrack acceptedTrack
    ) {

        public static Applicant from(RecruitingDecisionHistoryInfo.ApplicantInfo info) {
            return new Applicant(
                GlobalId.encode(GlobalIdTypes.CHAPTER, info.chapterId()),
                info.chapterName(),
                GlobalId.encode(GlobalIdTypes.SCHOOL, info.schoolId()),
                info.schoolName(),
                info.name(),
                info.firstChoice(),
                info.secondChoice(),
                info.acceptedTrack()
            );
        }
    }

    public record Decider(
        String memberId,
        String chapterId,
        String chapterName,
        String schoolId,
        String schoolName,
        ChallengerRoleType roleType,
        String name,
        String nickname
    ) {

        public static Decider from(RecruitingDecisionHistoryInfo.DeciderInfo info) {
            return new Decider(
                GlobalId.encode(GlobalIdTypes.MEMBER, info.memberId()),
                encodeNullable(GlobalIdTypes.CHAPTER, info.chapterId()),
                info.chapterName(),
                encodeNullable(GlobalIdTypes.SCHOOL, info.schoolId()),
                info.schoolName(),
                info.roleType(),
                info.name(),
                info.nickname()
            );
        }

        private static String encodeNullable(String typeName, Long rawId) {
            return rawId == null ? null : GlobalId.encode(typeName, rawId);
        }
    }
}
