package com.umc.product.curriculum.adapter.in.web.v2.dto.request;

import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.CreateOriginalWorkbookMissionCommand;
import com.umc.product.curriculum.domain.enums.MissionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOriginalWorkbookMissionRequest(
    @NotNull(message = "원본 워크북 ID는 필수입니다") Long originalWorkbookId,

    @NotBlank(message = "미션 제목은 필수입니다") String title,

    String description,

    @NotNull(message = "미션 유형은 필수입니다") MissionType missionType,

    @NotNull(message = "필수 여부는 필수입니다") Boolean isNecessary
) {
    public CreateOriginalWorkbookMissionCommand toCommand() {
        return CreateOriginalWorkbookMissionCommand.builder()
            .originalWorkbookId(originalWorkbookId)
            .title(title)
            .description(description)
            .missionType(missionType)
            .isNecessary(isNecessary)
            .build();
    }
}
