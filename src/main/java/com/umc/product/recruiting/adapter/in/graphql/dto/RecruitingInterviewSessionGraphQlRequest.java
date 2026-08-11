package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewSchedulesCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingInterviewSessionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeleteRecruitingInterviewSessionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingInterviewSessionCommand;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class RecruitingInterviewSessionGraphQlRequest {

    private RecruitingInterviewSessionGraphQlRequest() {
    }

    public record Create(
        String seasonId,
        String roundId,
        String name,
        Instant startsAt,
        Instant endsAt,
        @NotNull @Positive Integer slotDurationMinutes,
        RecruitingInterviewMode mode,
        String location
    ) {

        @AssertTrue(
            message = "지원자 1명당 면접 시간은 15분의 양의 배수여야 합니다."
        )
        public boolean isSlotDurationValid() {
            return slotDurationMinutes == null || slotDurationMinutes % 15 == 0;
        }

        public Long decodedSeasonId() {
            return GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON);
        }

        public Long decodedRoundId() {
            return GlobalId.decodeLong(roundId, GlobalIdTypes.RECRUITING_ROUND);
        }

        public CreateRecruitingInterviewSessionCommand toCommand(Long requesterMemberId) {
            return CreateRecruitingInterviewSessionCommand.of(
                decodedRoundId(), requesterMemberId, name, startsAt, endsAt, slotDurationMinutes, mode, location
            );
        }
    }

    public record Update(
        String seasonId,
        String roundId,
        String sessionId,
        String name,
        Instant startsAt,
        Instant endsAt,
        @NotNull @Positive Integer slotDurationMinutes,
        RecruitingInterviewMode mode,
        String location
    ) {

        @AssertTrue(
            message = "지원자 1명당 면접 시간은 15분의 양의 배수여야 합니다."
        )
        public boolean isSlotDurationValid() {
            return slotDurationMinutes == null || slotDurationMinutes % 15 == 0;
        }

        public Long decodedSeasonId() {
            return GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON);
        }

        public Long decodedRoundId() {
            return GlobalId.decodeLong(roundId, GlobalIdTypes.RECRUITING_ROUND);
        }

        public UpdateRecruitingInterviewSessionCommand toCommand(Long requesterMemberId) {
            return UpdateRecruitingInterviewSessionCommand.of(
                GlobalId.decodeLong(sessionId, GlobalIdTypes.RECRUITING_INTERVIEW_SESSION),
                decodedRoundId(),
                requesterMemberId,
                name,
                startsAt,
                endsAt,
                slotDurationMinutes,
                mode,
                location
            );
        }
    }

    public record Delete(
        String seasonId,
        String roundId,
        String sessionId
    ) {

        public Long decodedSeasonId() {
            return GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON);
        }

        public Long decodedRoundId() {
            return GlobalId.decodeLong(roundId, GlobalIdTypes.RECRUITING_ROUND);
        }

        public DeleteRecruitingInterviewSessionCommand toCommand(Long requesterMemberId) {
            return DeleteRecruitingInterviewSessionCommand.of(
                GlobalId.decodeLong(sessionId, GlobalIdTypes.RECRUITING_INTERVIEW_SESSION),
                decodedRoundId(),
                requesterMemberId
            );
        }
    }

    public record ConfirmSchedules(
        String seasonId,
        String roundId,
        @NotEmpty List<@NotNull @Valid Assignment> assignments
    ) {

        @AssertTrue(
            message = "면접 배정 수가 허용된 최대치를 초과했습니다."
        )
        public boolean isAssignmentCountValid() {
            return assignments == null
                || assignments.size() <= ConfirmRecruitingInterviewSchedulesCommand.MAX_ASSIGNMENT_COUNT;
        }

        public Long decodedSeasonId() {
            return GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON);
        }

        public Long decodedRoundId() {
            return GlobalId.decodeLong(roundId, GlobalIdTypes.RECRUITING_ROUND);
        }

        public ConfirmRecruitingInterviewSchedulesCommand toCommand(Long requesterMemberId) {
            return ConfirmRecruitingInterviewSchedulesCommand.of(
                decodedRoundId(),
                requesterMemberId,
                assignments.stream().map(Assignment::toCommand).toList()
            );
        }
    }

    public record Assignment(
        @NotNull String applicationId,
        @NotNull String sessionId,
        @NotNull Instant startsAt,
        @NotBlank @Size(max = 2000) String contactSnapshot
    ) {

        private ConfirmRecruitingInterviewSchedulesCommand.Assignment toCommand() {
            return ConfirmRecruitingInterviewSchedulesCommand.Assignment.of(
                GlobalId.decodeLong(applicationId, GlobalIdTypes.RECRUITING_APPLICATION),
                GlobalId.decodeLong(sessionId, GlobalIdTypes.RECRUITING_INTERVIEW_SESSION),
                startsAt,
                contactSnapshot
            );
        }
    }
}
