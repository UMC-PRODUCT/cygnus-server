package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundInterviewQuestionCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "면접 질문 생성 또는 수정 요청")
public record RecruitingInterviewQuestionRequest(
    @Schema(description = "면접 질문 본문", example = "지원 동기를 구체적으로 설명해 주세요.")
    @NotBlank @Size(max = 2000) String content,
    @Schema(description = "질문 노출 순서", example = "0")
    @NotNull @PositiveOrZero Integer orderNo
) {

    public CreateRecruitingRoundInterviewQuestionCommand toRoundCreateCommand(Long roundId, Long requesterMemberId) {
        return CreateRecruitingRoundInterviewQuestionCommand.of(roundId, requesterMemberId, content, orderNo);
    }

    public UpdateRecruitingRoundInterviewQuestionCommand toRoundUpdateCommand(
        Long questionId,
        Long roundId,
        Long requesterMemberId
    ) {
        return UpdateRecruitingRoundInterviewQuestionCommand.of(
            questionId,
            roundId,
            requesterMemberId,
            content,
            orderNo
        );
    }

    public CreateRecruitingApplicationInterviewQuestionCommand toApplicationCreateCommand(
        Long applicationId,
        Long requesterMemberId
    ) {
        return CreateRecruitingApplicationInterviewQuestionCommand.of(
            applicationId,
            requesterMemberId,
            content,
            orderNo
        );
    }

    public UpdateRecruitingApplicationInterviewQuestionCommand toApplicationUpdateCommand(
        Long questionId,
        Long applicationId,
        Long requesterMemberId
    ) {
        return UpdateRecruitingApplicationInterviewQuestionCommand.of(
            questionId,
            applicationId,
            requesterMemberId,
            content,
            orderNo
        );
    }
}
