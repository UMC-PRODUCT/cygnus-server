package com.umc.product.recruiting.application.port.in.query.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;

public record RecruitingInterviewScheduleBoardInfo(
    Long roundId,
    LocalDate date,
    List<SessionInfo> sessions,
    List<ApplicantInfo> pendingApplicants,
    List<ApplicantInfo> confirmedApplicants
) {

    public RecruitingInterviewScheduleBoardInfo {
        sessions = List.copyOf(sessions);
        pendingApplicants = List.copyOf(pendingApplicants);
        confirmedApplicants = List.copyOf(confirmedApplicants);
    }

    public record SessionInfo(
        Long sessionId,
        String name,
        Instant startsAt,
        Instant endsAt,
        RecruitingInterviewMode mode,
        String location,
        List<SlotInfo> slots
    ) {

        public SessionInfo {
            slots = List.copyOf(slots);
        }
    }

    public record SlotInfo(
        Instant startsAt,
        Instant endsAt,
        List<Long> availableApplicationIds,
        ApplicantInfo assignedApplicant
    ) {

        public SlotInfo {
            availableApplicationIds = List.copyOf(availableApplicationIds);
        }
    }

    public record ApplicantInfo(Long applicationId, String applicantName) {
    }
}
