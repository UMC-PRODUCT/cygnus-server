package com.umc.product.curriculum.adapter.in.web.v2.dto.response;

import java.time.Instant;
import java.util.List;

import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo;
import com.umc.product.curriculum.application.port.in.query.dto.MyCurriculumInfo.MissionSubmissionInfo;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.SubmissionStatus;

import lombok.Builder;

@Builder
public record MissionSubmissionResponse(
    Long missionSubmissionId,
    Long originalWorkbookMissionId,
    MissionType submittedAsType,
    String submittedContent,
    Instant submittedAt,
    Instant lastEditedAt,
    SubmissionStatus status,
    boolean hasFeedback,
    List<MissionFeedbackResponse> feedbacks
) {

    static MissionSubmissionResponse from(Long originalWorkbookMissionId, MissionSubmissionInfo info) {
        return MissionSubmissionResponse.builder()
            .missionSubmissionId(info.missionSubmissionId())
            .originalWorkbookMissionId(originalWorkbookMissionId)
            .submittedAsType(info.submittedAsType())
            .submittedContent(info.submittedContent())
            .submittedAt(info.submittedAt())
            .lastEditedAt(info.lastEditedAt())
            .status(info.status())
            .hasFeedback(info.hasFeedback())
            .feedbacks(info.feedbacks().stream()
                .map(MissionFeedbackResponse::from)
                .toList())
            .build();
    }

    public static MissionSubmissionResponse from(ChallengerWorkbookInfo.MissionSubmissionInfo info) {
        List<MissionFeedbackResponse> feedbacks = info.feedbacks().stream()
            .map(MissionFeedbackResponse::from)
            .toList();

        return MissionSubmissionResponse.builder()
            .missionSubmissionId(info.missionSubmissionId())
            .originalWorkbookMissionId(info.originalWorkbookMissionId())
            .submittedAsType(info.submittedAsType())
            .submittedContent(info.submittedContent())
            .submittedAt(info.submittedAt())
            .lastEditedAt(info.lastEditedAt())
            .status(resolveStatus(feedbacks))
            .hasFeedback(info.hasFeedback())
            .feedbacks(feedbacks)
            .build();
    }

    private static SubmissionStatus resolveStatus(List<MissionFeedbackResponse> feedbacks) {
        if (feedbacks.isEmpty()) {
            return SubmissionStatus.PENDING;
        }
        if (feedbacks.stream()
            .map(MissionFeedbackResponse::feedbackResult)
            .anyMatch(result -> result == FeedbackResult.FAIL)) {
            return SubmissionStatus.FAIL;
        }
        return feedbacks.stream()
            .map(MissionFeedbackResponse::feedbackResult)
            .anyMatch(result -> result == FeedbackResult.PASS)
            ? SubmissionStatus.PASS
            : SubmissionStatus.PENDING;
    }
}
