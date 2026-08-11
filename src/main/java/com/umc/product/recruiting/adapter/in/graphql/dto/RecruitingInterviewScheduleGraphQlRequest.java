package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RequestRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewAvailabilityCommand;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public final class RecruitingInterviewScheduleGraphQlRequest {

    private RecruitingInterviewScheduleGraphQlRequest() {
    }

    public record RequestAvailability(String applicationId, String contactSnapshot) {

        public RequestRecruitingInterviewScheduleCommand toCommand(Long requesterMemberId) {
            return RequestRecruitingInterviewScheduleCommand.of(
                GlobalId.decodeLong(applicationId, GlobalIdTypes.RECRUITING_APPLICATION),
                requesterMemberId,
                contactSnapshot
            );
        }
    }

    public record Submit(
        String applicationId,
        @NotEmpty List<@NotNull Instant> times
    ) {

        public SubmitRecruitingInterviewAvailabilityCommand toCommand(Long requesterMemberId) {
            return SubmitRecruitingInterviewAvailabilityCommand.of(
                GlobalId.decodeLong(applicationId, GlobalIdTypes.RECRUITING_APPLICATION),
                requesterMemberId,
                times
            );
        }
    }

    public record Confirm(
        String applicationId,
        @NotNull String sessionId,
        Instant startsAt,
        Instant endsAt,
        String location,
        String contactSnapshot
    ) {

        public ConfirmRecruitingInterviewScheduleCommand toCommand(Long requesterMemberId) {
            return ConfirmRecruitingInterviewScheduleCommand.of(
                GlobalId.decodeLong(applicationId, GlobalIdTypes.RECRUITING_APPLICATION),
                requesterMemberId,
                GlobalId.decodeLong(sessionId, GlobalIdTypes.RECRUITING_INTERVIEW_SESSION),
                startsAt,
                endsAt,
                location,
                contactSnapshot
            );
        }
    }
}
