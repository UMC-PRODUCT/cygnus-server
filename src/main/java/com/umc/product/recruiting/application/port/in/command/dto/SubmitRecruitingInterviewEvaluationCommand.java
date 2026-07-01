package com.umc.product.recruiting.application.port.in.command.dto;

import lombok.Builder;

@Builder
public record SubmitRecruitingInterviewEvaluationCommand(
    Long assignmentId,
    Long evaluatorMemberId,
    Integer score,
    String comment
) {
}
