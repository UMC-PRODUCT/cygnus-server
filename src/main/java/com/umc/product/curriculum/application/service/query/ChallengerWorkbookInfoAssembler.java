package com.umc.product.curriculum.application.service.query;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo;
import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo.MissionFeedbackInfo;
import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo.MissionSubmissionInfo;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.curriculum.domain.MissionSubmission;

final class ChallengerWorkbookInfoAssembler {

    private ChallengerWorkbookInfoAssembler() {
    }

    static ChallengerWorkbookInfo toInfo(
        ChallengerWorkbook workbook,
        List<MissionSubmission> submissions,
        Map<Long, List<MissionFeedback>> feedbacksBySubmissionId,
        Set<Long> requiredMissionIds,
        boolean isBestWorkbook
    ) {
        return ChallengerWorkbookInfo.builder()
            .challengerWorkbookId(workbook.getId())
            .originalWorkbookId(workbook.getOriginalWorkbook().getId())
            .receivedStudyGroupId(workbook.getStudyGroupId())
            .challengerId(workbook.getMemberId())
            .isExcused(workbook.isExcused())
            .excusedReason(workbook.getExcusedReason())
            .content(workbook.getContent())
            .isBestWorkbook(isBestWorkbook)
            .requiredMissionIds(requiredMissionIds)
            .submissions(submissions.stream()
                .map(submission -> toSubmissionInfo(
                    submission,
                    feedbacksBySubmissionId.getOrDefault(submission.getId(), List.of())
                ))
                .toList())
            .build();
    }

    private static MissionSubmissionInfo toSubmissionInfo(
        MissionSubmission submission,
        List<MissionFeedback> feedbacks
    ) {
        return MissionSubmissionInfo.builder()
            .missionSubmissionId(submission.getId())
            .originalWorkbookMissionId(submission.getOriginalWorkbookMission().getId())
            .submittedAsType(submission.getSubmittedAsType())
            .submittedContent(submission.getContent())
            .submittedAt(submission.getCreatedAt())
            .lastEditedAt(submission.getUpdatedAt())
            .hasFeedback(!feedbacks.isEmpty())
            .feedbacks(feedbacks.stream().map(ChallengerWorkbookInfoAssembler::toFeedbackInfo).toList())
            .build();
    }

    private static MissionFeedbackInfo toFeedbackInfo(MissionFeedback feedback) {
        return MissionFeedbackInfo.builder()
            .missionFeedbackId(feedback.getId())
            .reviewerMemberId(feedback.getReviewerMemberId())
            .content(feedback.getContent())
            .feedbackResult(feedback.getFeedbackResult())
            .build();
    }
}
