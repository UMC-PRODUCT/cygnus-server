package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewScheduleBoardInfo;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;

public record RecruitingInterviewScheduleBoardGraphQlResponse(
    String roundId,
    LocalDate date,
    List<Session> sessions,
    List<Applicant> pendingApplicants,
    List<Applicant> confirmedApplicants
) {

    public static RecruitingInterviewScheduleBoardGraphQlResponse from(RecruitingInterviewScheduleBoardInfo info) {
        return new RecruitingInterviewScheduleBoardGraphQlResponse(
            GlobalId.encode(GlobalIdTypes.RECRUITING_ROUND, info.roundId()),
            info.date(),
            info.sessions().stream().map(Session::from).toList(),
            info.pendingApplicants().stream().map(Applicant::from).toList(),
            info.confirmedApplicants().stream().map(Applicant::from).toList()
        );
    }

    public record Session(
        String sessionId,
        String name,
        Instant startsAt,
        Instant endsAt,
        RecruitingInterviewMode mode,
        String location,
        List<Slot> slots
    ) {

        private static Session from(RecruitingInterviewScheduleBoardInfo.SessionInfo info) {
            return new Session(
                GlobalId.encode(GlobalIdTypes.RECRUITING_INTERVIEW_SESSION, info.sessionId()),
                info.name(),
                info.startsAt(),
                info.endsAt(),
                info.mode(),
                info.location(),
                info.slots().stream().map(Slot::from).toList()
            );
        }
    }

    public record Slot(
        Instant startsAt,
        Instant endsAt,
        List<String> availableApplicationIds,
        Applicant assignedApplicant
    ) {

        private static Slot from(RecruitingInterviewScheduleBoardInfo.SlotInfo info) {
            return new Slot(
                info.startsAt(),
                info.endsAt(),
                info.availableApplicationIds().stream()
                    .map(applicationId -> GlobalId.encode(GlobalIdTypes.RECRUITING_APPLICATION, applicationId))
                    .toList(),
                info.assignedApplicant() == null ? null : Applicant.from(info.assignedApplicant())
            );
        }
    }

    public record Applicant(String applicationId, String applicantName) {

        private static Applicant from(RecruitingInterviewScheduleBoardInfo.ApplicantInfo info) {
            return new Applicant(
                GlobalId.encode(GlobalIdTypes.RECRUITING_APPLICATION, info.applicationId()),
                info.applicantName()
            );
        }
    }
}
