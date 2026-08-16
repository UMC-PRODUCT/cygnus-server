package com.umc.product.curriculum.adapter.in.web.v2.dto.response;

public record CreateOriginalWorkbookMissionResponse(
    Long originalWorkbookMissionId
) {

    public static CreateOriginalWorkbookMissionResponse from(Long originalWorkbookMissionId) {
        return new CreateOriginalWorkbookMissionResponse(originalWorkbookMissionId);
    }
}
