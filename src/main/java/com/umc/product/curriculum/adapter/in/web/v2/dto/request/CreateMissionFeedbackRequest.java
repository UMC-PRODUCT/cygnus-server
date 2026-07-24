package com.umc.product.curriculum.adapter.in.web.v2.dto.request;

import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.CreateMissionFeedbackCommand;
import com.umc.product.curriculum.domain.enums.FeedbackResult;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMissionFeedbackRequest(
    @NotNull(message = "미션 제출물 ID는 필수입니다") Long missionSubmissionId,
    @NotBlank(message = "피드백 내용은 필수입니다") String content,
    @NotNull(message = "평가 결과는 필수입니다") FeedbackResult result
) {
    public CreateMissionFeedbackCommand toCommand(Long reviewerMemberId) {
        return CreateMissionFeedbackCommand.builder()
            .missionSubmissionId(missionSubmissionId)
            .reviewerMemberId(reviewerMemberId)
            .content(content)
            .result(result)
            .build();
    }
}
