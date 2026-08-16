package com.umc.product.curriculum.adapter.in.web.v2.dto.response;

import com.umc.product.curriculum.application.port.in.query.dto.OriginalWorkbookInfo;
import com.umc.product.curriculum.domain.enums.MissionType;

public record OriginalWorkbookMissionResponse(
    Long originalWorkbookId,
    Long originalWorkbookMissionId,
    String title,
    String description,
    MissionType missionType,
    boolean isNecessary
) {
    public static OriginalWorkbookMissionResponse from(
        Long originalWorkbookId,
        OriginalWorkbookInfo.OriginalWorkbookMissionInfo info
    ) {
        return new OriginalWorkbookMissionResponse(
            originalWorkbookId, info.originalWorkbookMissionId(), info.title(), info.description(),
            info.missionType(), info.isNecessary()
        );
    }
}
