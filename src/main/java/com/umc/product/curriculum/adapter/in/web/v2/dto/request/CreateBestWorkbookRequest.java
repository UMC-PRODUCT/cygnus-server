package com.umc.product.curriculum.adapter.in.web.v2.dto.request;

import com.umc.product.curriculum.application.port.in.command.dto.workbook.CreateWeeklyBestWorkbookCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBestWorkbookRequest(
    @NotNull Long bestMemberId,
    @NotNull Long weeklyCurriculumId,
    @NotNull Long studyGroupId,
    @NotBlank String reason
) {
    public CreateWeeklyBestWorkbookCommand toCommand(Long decidedMemberId) {
        return CreateWeeklyBestWorkbookCommand.builder()
            .decidedMemberId(decidedMemberId)
            .bestMemberId(bestMemberId)
            .weeklyCurriculumId(weeklyCurriculumId)
            .studyGroupId(studyGroupId)
            .reason(reason)
            .build();
    }
}
