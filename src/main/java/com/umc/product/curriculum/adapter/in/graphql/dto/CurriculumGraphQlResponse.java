package com.umc.product.curriculum.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumOverviewInfo;
import com.umc.product.curriculum.application.port.in.query.dto.MyCurriculumInfo;
import com.umc.product.curriculum.application.port.in.query.dto.OriginalWorkbookInfo;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.enums.SubmissionStatus;

public record CurriculumGraphQlResponse(
    Long curriculumId,
    String title,
    List<Week> weeks
) {

    public static CurriculumGraphQlResponse from(CurriculumOverviewInfo info) {
        return new CurriculumGraphQlResponse(
            info.curriculumId(),
            info.title(),
            info.weeks().stream().map(Week::from).toList()
        );
    }

    public static CurriculumGraphQlResponse from(MyCurriculumInfo info) {
        return new CurriculumGraphQlResponse(
            info.curriculumId(),
            info.title(),
            info.weeks().stream().map(Week::from).toList()
        );
    }

    public record Week(
        Long weeklyCurriculumId,
        Long weekNo,
        String title,
        boolean extra,
        Instant startsAt,
        Instant endsAt,
        List<Workbook> workbooks
    ) {

        private static Week from(CurriculumOverviewInfo.WeeklyCurriculumOverviewInfo info) {
            return new Week(
                info.weeklyCurriculumId(),
                info.weekNo(),
                info.title(),
                info.isExtra(),
                info.startsAt(),
                info.endsAt(),
                List.of()
            );
        }

        private static Week from(MyCurriculumInfo.MyWeeklyCurriculumInfo info) {
            return new Week(
                info.weeklyCurriculumId(),
                info.weekNo(),
                info.title(),
                info.isExtra(),
                info.startsAt(),
                info.endsAt(),
                info.releasedOriginalWorkbooks().stream().map(Workbook::from).toList()
            );
        }
    }

    public record Workbook(
        Long originalWorkbookId,
        String title,
        String description,
        String url,
        String content,
        OriginalWorkbookType type,
        OriginalWorkbookStatus status,
        Instant releasedAt,
        Long releasedMemberId,
        List<Mission> missions,
        Long challengerWorkbookId
    ) {

        public static Workbook from(OriginalWorkbookInfo info) {
            return new Workbook(
                info.originalWorkbookId(),
                info.title(),
                info.description(),
                info.url(),
                info.content(),
                info.type(),
                info.status(),
                info.releasedAt(),
                info.releasedMemberId(),
                info.missions().stream().map(Mission::from).toList(),
                null
            );
        }

        private static Workbook from(MyCurriculumInfo.MyOriginalWorkbookInfo info) {
            return new Workbook(
                info.originalWorkbookId(),
                info.title(),
                info.description(),
                info.url(),
                null,
                info.type(),
                null,
                null,
                null,
                info.missions().stream().map(Mission::from).toList(),
                info.challengerWorkbookId().orElse(null)
            );
        }
    }

    public record Mission(
        Long originalWorkbookMissionId,
        String title,
        String description,
        MissionType missionType,
        boolean necessary,
        Submission submission
    ) {

        private static Mission from(OriginalWorkbookInfo.OriginalWorkbookMissionInfo info) {
            return new Mission(
                info.originalWorkbookMissionId(),
                info.title(),
                info.description(),
                info.missionType(),
                info.isNecessary(),
                null
            );
        }

        private static Mission from(MyCurriculumInfo.MyOriginalWorkbookMissionInfo info) {
            return new Mission(
                info.originalWorkbookMissionId(),
                info.title(),
                info.description(),
                info.missionType(),
                info.isNecessary(),
                info.submission().map(Submission::from).orElse(null)
            );
        }
    }

    public record ChallengerWorkbook(
        Long challengerWorkbookId,
        Long originalWorkbookId,
        Long receivedStudyGroupId,
        Long challengerId,
        boolean excused,
        String excusedReason,
        String content,
        boolean bestWorkbook,
        List<Submission> submissions
    ) {

        public static ChallengerWorkbook from(ChallengerWorkbookInfo info) {
            return new ChallengerWorkbook(
                info.challengerWorkbookId(),
                info.originalWorkbookId(),
                info.receivedStudyGroupId(),
                info.challengerId(),
                info.isExcused(),
                info.excusedReason(),
                info.content(),
                info.isBestWorkbook(),
                info.submissions().stream().map(Submission::from).toList()
            );
        }
    }

    public record Submission(
        Long missionSubmissionId,
        Long originalWorkbookMissionId,
        MissionType submittedAsType,
        String submittedContent,
        Instant submittedAt,
        Instant lastEditedAt,
        SubmissionStatus status,
        List<Feedback> feedbacks
    ) {

        private static Submission from(MyCurriculumInfo.MissionSubmissionInfo info) {
            return new Submission(
                info.missionSubmissionId(),
                null,
                info.submittedAsType(),
                info.submittedContent(),
                info.submittedAt(),
                info.lastEditedAt(),
                info.status(),
                info.feedbacks().stream().map(Feedback::from).toList()
            );
        }

        private static Submission from(ChallengerWorkbookInfo.MissionSubmissionInfo info) {
            return new Submission(
                info.missionSubmissionId(),
                info.originalWorkbookMissionId(),
                info.submittedAsType(),
                info.submittedContent(),
                info.submittedAt(),
                info.lastEditedAt(),
                null,
                info.feedbacks().stream().map(Feedback::from).toList()
            );
        }
    }

    public record Feedback(
        Long missionFeedbackId,
        Long reviewerMemberId,
        String content,
        FeedbackResult feedbackResult
    ) {

        private static Feedback from(MyCurriculumInfo.MissionFeedbackInfo info) {
            return new Feedback(
                info.missionFeedbackId(),
                info.reviewerMemberId(),
                info.content(),
                info.feedbackResult()
            );
        }

        private static Feedback from(ChallengerWorkbookInfo.MissionFeedbackInfo info) {
            return new Feedback(
                info.missionFeedbackId(),
                info.reviewerMemberId(),
                info.content(),
                info.feedbackResult()
            );
        }
    }
}
