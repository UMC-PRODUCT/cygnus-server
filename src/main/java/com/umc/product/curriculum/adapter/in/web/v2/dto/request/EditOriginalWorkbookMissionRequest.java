package com.umc.product.curriculum.adapter.in.web.v2.dto.request;

import com.umc.product.curriculum.application.port.in.command.dto.workbook.EditOriginalWorkbookMissionCommand;
import com.umc.product.curriculum.domain.enums.MissionType;

public record EditOriginalWorkbookMissionRequest(
    String title,
    String description,
    MissionType missionType,
    Boolean isNecessary
) {
    public EditOriginalWorkbookMissionCommand toCommand(Long originalWorkbookMissionId) {
        return EditOriginalWorkbookMissionCommand.builder()
            .originalWorkbookMissionId(originalWorkbookMissionId)
            .title(title)
            .description(description)
            .missionType(missionType)
            .isNecessary(isNecessary)
            .build();
    }
}
