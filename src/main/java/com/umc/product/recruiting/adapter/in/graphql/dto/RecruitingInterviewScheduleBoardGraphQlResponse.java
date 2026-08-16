package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewScheduleBoardInfo;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;

public record RecruitingInterviewScheduleBoardGraphQlResponse(
    Long roundId,
    LocalDate date,
    List<Session> sessions,
    List<Applicant> pendingApplicants,
    List<Applicant> confirmedApplicants
) {

    public static RecruitingInterviewScheduleBoardGraphQlResponse from(RecruitingInterviewScheduleBoardInfo info) {
        return new RecruitingInterviewScheduleBoardGraphQlResponse(
            info.roundId(),
            info.date(),
            info.sessions().stream().map(Session::from).toList(),
            info.pendingApplicants().stream().map(Applicant::from).toList(),
            info.confirmedApplicants().stream().map(Applicant::from).toList()
        );
    }

    public record Session(
        Long sessionId,
        String name,
        Instant startsAt,
        Instant endsAt,
        RecruitingInterviewMode mode,
        String location,
        List<Slot> slots
    ) {

        private static Session from(RecruitingInterviewScheduleBoardInfo.SessionInfo info) {
            return new Session(
                info.sessionId(), info.name(), info.startsAt(), info.endsAt(), info.mode(), info.location(),
                info.slots().stream().map(Slot::from).toList()
            );
        }
    }

    public record Slot(Instant startsAt, Instant endsAt, List<Long> availableApplicationIds, Applicant assignedApplicant) {

        private static Slot from(RecruitingInterviewScheduleBoardInfo.SlotInfo info) {
            return new Slot(
                info.startsAt(),
                info.endsAt(),
                info.availableApplicationIds(),
                info.assignedApplicant() == null ? null : Applicant.from(info.assignedApplicant())
            );
        }
    }

    public record Applicant(Long applicationId, String applicantName) {

        private static Applicant from(RecruitingInterviewScheduleBoardInfo.ApplicantInfo info) {
            return new Applicant(info.applicationId(), info.applicantName());
        }
    }
}
