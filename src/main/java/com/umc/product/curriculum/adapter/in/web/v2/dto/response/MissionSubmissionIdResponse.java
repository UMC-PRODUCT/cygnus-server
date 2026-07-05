package com.umc.product.curriculum.adapter.in.web.v2.dto.response;

public record MissionSubmissionIdResponse(
    Long missionSubmissionId
) {
    public static MissionSubmissionIdResponse from(Long missionSubmissionId) {
        return new MissionSubmissionIdResponse(missionSubmissionId);
    }
}
