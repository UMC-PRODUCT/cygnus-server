package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RequestRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewAvailabilityCommand;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public final class RecruitingInterviewScheduleGraphQlRequest {

    private RecruitingInterviewScheduleGraphQlRequest() {
    }

    public record RequestAvailability(String contactSnapshot) {

        public RequestRecruitingInterviewScheduleCommand toCommand(Long applicationId, Long requesterMemberId) {
            return RequestRecruitingInterviewScheduleCommand.of(applicationId, requesterMemberId, contactSnapshot);
        }
    }

    public record Submit(
        @NotEmpty List<@NotNull Instant> times
    ) {

        public SubmitRecruitingInterviewAvailabilityCommand toCommand(Long applicationId, Long requesterMemberId) {
            return SubmitRecruitingInterviewAvailabilityCommand.of(applicationId, requesterMemberId, times);
        }
    }

    public record Confirm(
        @NotNull Long sessionId,
        Instant startsAt,
        Instant endsAt,
        String location,
        String contactSnapshot
    ) {

        public ConfirmRecruitingInterviewScheduleCommand toCommand(Long applicationId, Long requesterMemberId) {
            return ConfirmRecruitingInterviewScheduleCommand.of(
                applicationId,
                requesterMemberId,
                sessionId,
                startsAt,
                endsAt,
                location,
                contactSnapshot
            );
        }
    }
}
