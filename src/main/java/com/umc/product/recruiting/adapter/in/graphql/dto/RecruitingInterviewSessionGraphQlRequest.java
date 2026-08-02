package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewSchedulesCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingInterviewSessionCommand;
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

    public record Session(
        String name,
        Instant startsAt,
        Instant endsAt,
        @NotNull @Positive Integer slotDurationMinutes,
        RecruitingInterviewMode mode,
        String location
    ) {

        @AssertTrue(message = "지원자 1명당 면접 시간은 15분의 양의 배수여야 합니다.") public boolean isSlotDurationValid() {
            return slotDurationMinutes == null || slotDurationMinutes % 15 == 0;
        }

        public CreateRecruitingInterviewSessionCommand toCreateCommand(Long roundId, Long requesterMemberId) {
            return CreateRecruitingInterviewSessionCommand.of(
                roundId, requesterMemberId, name, startsAt, endsAt, slotDurationMinutes, mode, location
            );
        }

        public UpdateRecruitingInterviewSessionCommand toUpdateCommand(
            Long sessionId,
            Long roundId,
            Long requesterMemberId
        ) {
            return UpdateRecruitingInterviewSessionCommand.of(
                sessionId, roundId, requesterMemberId, name, startsAt, endsAt, slotDurationMinutes, mode, location
            );
        }
    }

    public record ConfirmSchedules(
        @NotEmpty @Size(max = ConfirmRecruitingInterviewSchedulesCommand.MAX_ASSIGNMENT_COUNT) List<@NotNull @Valid Assignment> assignments
    ) {

        public ConfirmRecruitingInterviewSchedulesCommand toCommand(Long roundId, Long requesterMemberId) {
            return ConfirmRecruitingInterviewSchedulesCommand.of(
                roundId,
                requesterMemberId,
                assignments.stream().map(Assignment::toCommand).toList()
            );
        }
    }

    public record Assignment(
        @NotNull @Positive Long applicationId,
        @NotNull @Positive Long sessionId,
        @NotNull Instant startsAt,
        @NotBlank @Size(max = 2000) String contactSnapshot
    ) {

        private ConfirmRecruitingInterviewSchedulesCommand.Assignment toCommand() {
            return ConfirmRecruitingInterviewSchedulesCommand.Assignment.of(
                applicationId, sessionId, startsAt, contactSnapshot
            );
        }
    }
}
