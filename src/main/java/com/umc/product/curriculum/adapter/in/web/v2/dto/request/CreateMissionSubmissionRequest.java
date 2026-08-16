package com.umc.product.curriculum.adapter.in.web.v2.dto.request;

import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.CreateMissionSubmissionCommand;

import jakarta.validation.constraints.NotNull;

public record CreateMissionSubmissionRequest(
    @NotNull(message = "원본 워크북 미션 ID는 필수입니다") Long originalWorkbookMissionId,
    @NotNull(message = "챌린저 워크북 ID는 필수입니다") Long challengerMissionId,
    String content
) {
    public CreateMissionSubmissionCommand toCommand(Long requesterMemberId) {
        return CreateMissionSubmissionCommand.builder()
            .originalWorkbookMissionId(originalWorkbookMissionId)
            .challengerWorkbookId(challengerMissionId)
            .requesterMemberId(requesterMemberId)
            .content(content)
            .build();
    }
}
