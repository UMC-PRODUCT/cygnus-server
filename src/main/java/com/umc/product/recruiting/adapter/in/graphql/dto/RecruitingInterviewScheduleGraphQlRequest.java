package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;

import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RequestRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewAvailabilityCommand;

public final class RecruitingInterviewScheduleGraphQlRequest {

    private RecruitingInterviewScheduleGraphQlRequest() {
    }

    public record RequestAvailability(String contactSnapshot) {

        public RequestRecruitingInterviewScheduleCommand toCommand(Long applicationId, Long requesterMemberId) {
            return RequestRecruitingInterviewScheduleCommand.of(applicationId, requesterMemberId, contactSnapshot);
        }
    }

    public record SubmitAvailability(Long availabilityFormResponseId) {

        public SubmitRecruitingInterviewAvailabilityCommand toCommand(
            Long applicationId,
            Long requesterMemberId
        ) {
            return SubmitRecruitingInterviewAvailabilityCommand.of(
                applicationId,
                requesterMemberId,
                availabilityFormResponseId
            );
        }
    }

    public record Confirm(
        Instant startsAt,
        Instant endsAt,
        String location,
        String contactSnapshot
    ) {

        public ConfirmRecruitingInterviewScheduleCommand toCommand(Long applicationId, Long requesterMemberId) {
            return ConfirmRecruitingInterviewScheduleCommand.of(
                applicationId,
                requesterMemberId,
                startsAt,
                endsAt,
                location,
                contactSnapshot
            );
        }
    }
}
