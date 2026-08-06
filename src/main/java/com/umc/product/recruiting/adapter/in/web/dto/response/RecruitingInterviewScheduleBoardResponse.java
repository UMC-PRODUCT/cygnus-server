package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewScheduleBoardInfo;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "운영진 면접 일정 보드 응답")
public record RecruitingInterviewScheduleBoardResponse(
    @Schema(description = "Round ID", example = "3") Long roundId,
    @Schema(description = "KST 기준 조회 날짜") LocalDate date,
    @Schema(description = "세션별 계산 슬롯") List<SessionResponse> sessions,
    @Schema(description = "배정 대기 지원자") List<ApplicantResponse> pendingApplicants,
    @Schema(description = "확정 지원자") List<ApplicantResponse> confirmedApplicants
) {
    public static RecruitingInterviewScheduleBoardResponse from(RecruitingInterviewScheduleBoardInfo info) {
        return new RecruitingInterviewScheduleBoardResponse(
            info.roundId(), info.date(), info.sessions().stream().map(SessionResponse::from).toList(),
            info.pendingApplicants().stream().map(ApplicantResponse::from).toList(),
            info.confirmedApplicants().stream().map(ApplicantResponse::from).toList()
        );
    }

    @Schema(description = "세션 보드 정보")
    public record SessionResponse(Long sessionId, String name, Instant startsAt, Instant endsAt,
                                  RecruitingInterviewMode mode, String location, List<SlotResponse> slots) {
        private static SessionResponse from(RecruitingInterviewScheduleBoardInfo.SessionInfo info) {
            return new SessionResponse(info.sessionId(), info.name(), info.startsAt(), info.endsAt(), info.mode(),
                info.location(), info.slots().stream().map(SlotResponse::from).toList());
        }
    }

    @Schema(description = "서버 계산 면접 슬롯")
    public record SlotResponse(Instant startsAt, Instant endsAt, List<Long> availableApplicationIds,
                               ApplicantResponse assignedApplicant) {
        private static SlotResponse from(RecruitingInterviewScheduleBoardInfo.SlotInfo info) {
            return new SlotResponse(info.startsAt(), info.endsAt(), info.availableApplicationIds(),
                info.assignedApplicant() == null ? null : ApplicantResponse.from(info.assignedApplicant()));
        }
    }

    @Schema(description = "보드 지원자 정보")
    public record ApplicantResponse(Long applicationId, String applicantName) {
        private static ApplicantResponse from(RecruitingInterviewScheduleBoardInfo.ApplicantInfo info) {
            return new ApplicantResponse(info.applicationId(), info.applicantName());
        }
    }
}
