package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewSchedulesCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingInterviewSessionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingInterviewSessionCommand;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;

public final class RecruitingInterviewSessionGraphQlRequest {

    private RecruitingInterviewSessionGraphQlRequest() {
    }

    public record Session(String name, Instant startsAt, Instant endsAt, RecruitingInterviewMode mode, String location) {

        public CreateRecruitingInterviewSessionCommand toCreateCommand(Long roundId, Long requesterMemberId) {
            return CreateRecruitingInterviewSessionCommand.of(
                roundId, requesterMemberId, name, startsAt, endsAt, mode, location
            );
        }

        public UpdateRecruitingInterviewSessionCommand toUpdateCommand(
            Long sessionId,
            Long roundId,
            Long requesterMemberId
        ) {
            return UpdateRecruitingInterviewSessionCommand.of(
                sessionId, roundId, requesterMemberId, name, startsAt, endsAt, mode, location
            );
        }
    }

    public record ConfirmSchedules(List<Assignment> assignments) {

        public ConfirmRecruitingInterviewSchedulesCommand toCommand(Long roundId, Long requesterMemberId) {
            return ConfirmRecruitingInterviewSchedulesCommand.of(
                roundId,
                requesterMemberId,
                assignments.stream().map(Assignment::toCommand).toList()
            );
        }
    }

    public record Assignment(Long applicationId, Long sessionId, Instant startsAt, String contactSnapshot) {

        private ConfirmRecruitingInterviewSchedulesCommand.Assignment toCommand() {
            return ConfirmRecruitingInterviewSchedulesCommand.Assignment.of(
                applicationId, sessionId, startsAt, contactSnapshot
            );
        }
    }
}
