package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeactivateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeactivateRecruitingRoundInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundInterviewQuestionCommand;

public final class RecruitingInterviewQuestionGraphQlRequest {

    private RecruitingInterviewQuestionGraphQlRequest() {
    }

    public record RoundCreate(String roundId, String content, Integer orderNo) {

        public CreateRecruitingRoundInterviewQuestionCommand toCommand(Long requesterMemberId) {
            return CreateRecruitingRoundInterviewQuestionCommand.of(
                GlobalId.decodeLong(roundId, GlobalIdTypes.RECRUITING_ROUND),
                requesterMemberId,
                content,
                orderNo
            );
        }
    }

    public record RoundUpdate(String roundId, String questionId, String content, Integer orderNo) {

        public UpdateRecruitingRoundInterviewQuestionCommand toCommand(Long requesterMemberId) {
            return UpdateRecruitingRoundInterviewQuestionCommand.of(
                GlobalId.decodeLong(questionId, GlobalIdTypes.RECRUITING_INTERVIEW_QUESTION),
                GlobalId.decodeLong(roundId, GlobalIdTypes.RECRUITING_ROUND),
                requesterMemberId,
                content,
                orderNo
            );
        }
    }

    public record RoundDeactivate(String roundId, String questionId) {

        public DeactivateRecruitingRoundInterviewQuestionCommand toCommand(Long requesterMemberId) {
            return DeactivateRecruitingRoundInterviewQuestionCommand.of(
                GlobalId.decodeLong(questionId, GlobalIdTypes.RECRUITING_INTERVIEW_QUESTION),
                GlobalId.decodeLong(roundId, GlobalIdTypes.RECRUITING_ROUND),
                requesterMemberId
            );
        }
    }

    public record ApplicationCreate(String applicationId, String content, Integer orderNo) {

        public CreateRecruitingApplicationInterviewQuestionCommand toCommand(Long requesterMemberId) {
            return CreateRecruitingApplicationInterviewQuestionCommand.of(
                GlobalId.decodeLong(applicationId, GlobalIdTypes.RECRUITING_APPLICATION),
                requesterMemberId,
                content,
                orderNo
            );
        }
    }

    public record ApplicationUpdate(String applicationId, String questionId, String content, Integer orderNo) {

        public UpdateRecruitingApplicationInterviewQuestionCommand toCommand(Long requesterMemberId) {
            return UpdateRecruitingApplicationInterviewQuestionCommand.of(
                GlobalId.decodeLong(questionId, GlobalIdTypes.RECRUITING_INTERVIEW_QUESTION),
                GlobalId.decodeLong(applicationId, GlobalIdTypes.RECRUITING_APPLICATION),
                requesterMemberId,
                content,
                orderNo
            );
        }
    }

    public record ApplicationDeactivate(String applicationId, String questionId) {

        public DeactivateRecruitingApplicationInterviewQuestionCommand toCommand(Long requesterMemberId) {
            return DeactivateRecruitingApplicationInterviewQuestionCommand.of(
                GlobalId.decodeLong(questionId, GlobalIdTypes.RECRUITING_INTERVIEW_QUESTION),
                GlobalId.decodeLong(applicationId, GlobalIdTypes.RECRUITING_APPLICATION),
                requesterMemberId
            );
        }
    }
}
