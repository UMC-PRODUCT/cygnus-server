package com.umc.product.curriculum.adapter.in.web.v2.dto.response;

public record MissionFeedbackIdResponse(
    Long missionFeedbackId
) {
    public static MissionFeedbackIdResponse from(Long missionFeedbackId) {
        return new MissionFeedbackIdResponse(missionFeedbackId);
    }
}
