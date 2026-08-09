package com.umc.product.curriculum.adapter.in.web.v2.dto.response;

import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo;
import com.umc.product.curriculum.application.port.in.query.dto.MyCurriculumInfo.MissionFeedbackInfo;
import com.umc.product.curriculum.domain.enums.FeedbackResult;

import lombok.Builder;

@Builder
public record MissionFeedbackResponse(
    Long missionFeedbackId,
    Long reviewerMemberId,
    String content,
    FeedbackResult feedbackResult
) {

    static MissionFeedbackResponse from(MissionFeedbackInfo info) {
        return MissionFeedbackResponse.builder()
            .missionFeedbackId(info.missionFeedbackId())
            .reviewerMemberId(info.reviewerMemberId())
            .content(info.content())
            .feedbackResult(info.feedbackResult())
            .build();
    }

    public static MissionFeedbackResponse from(ChallengerWorkbookInfo.MissionFeedbackInfo info) {
        return MissionFeedbackResponse.builder()
            .missionFeedbackId(info.missionFeedbackId())
            .reviewerMemberId(info.reviewerMemberId())
            .content(info.content())
            .feedbackResult(info.feedbackResult())
            .build();
    }
}
