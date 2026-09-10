package com.umc.product.curriculum.application.port.in.query.dto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.enums.SubmissionStatus;

import lombok.Builder;

@Builder
public record MyCurriculumInfo(
    Long curriculumId,
    String title,
    List<MyWeeklyCurriculumInfo> weeks,
    ChallengerPart part,
    ChallengerTrack track
) {

    public MyCurriculumInfo(Long curriculumId, String title, List<MyWeeklyCurriculumInfo> weeks) {
        this(curriculumId, title, weeks, null, null);
    }

    public static MyCurriculumInfo of(CurriculumProjection projection, List<MyWeeklyCurriculumInfo> weeks) {
        return MyCurriculumInfo.builder()
            .curriculumId(projection.id())
            .title(projection.title())
            .part(projection.part())
            .track(projection.track())
            .weeks(weeks)
            .build();
    }

    @Builder
    public record MyWeeklyCurriculumInfo(
        Long weeklyCurriculumId,
        Long weekNo,
        String title,
        boolean isExtra,
        Instant startsAt,
        Instant endsAt,
        List<MyOriginalWorkbookInfo> releasedOriginalWorkbooks
    ) {
        public static MyWeeklyCurriculumInfo of(WeeklyCurriculum wc, List<MyOriginalWorkbookInfo> releasedOriginalWorkbooks) {
            return MyWeeklyCurriculumInfo.builder()
                .weeklyCurriculumId(wc.getId())
                .weekNo(wc.getWeekNo())
                .title(wc.getTitle())
                .isExtra(wc.isExtra())
                .startsAt(wc.getStartsAt())
                .endsAt(wc.getEndsAt())
                .releasedOriginalWorkbooks(releasedOriginalWorkbooks)
                .build();
        }
    }

    @Builder
    public record MyOriginalWorkbookInfo(
        Long originalWorkbookId,
        String title,
        String description,
        String url,
        OriginalWorkbookType type,
        List<MyOriginalWorkbookMissionInfo> missions,
        Optional<Long> challengerWorkbookId
    ) {
        public static MyOriginalWorkbookInfo of(
            OriginalWorkbook wb,
            List<MyOriginalWorkbookMissionInfo> missions,
            Optional<Long> challengerWorkbookId
        ) {
            return MyOriginalWorkbookInfo.builder()
                .originalWorkbookId(wb.getId())
                .title(wb.getTitle())
                .description(wb.getDescription())
                .url(wb.getUrl())
                .type(wb.getType())
                .missions(missions)
                .challengerWorkbookId(challengerWorkbookId)
                .build();
        }

        public boolean isDeployedToMember() {
            return challengerWorkbookId.isPresent();
        }
    }

    @Builder
    public record MyOriginalWorkbookMissionInfo(
        Long originalWorkbookMissionId,
        String title,
        String description,
        MissionType missionType,
        boolean isNecessary,
        boolean hasSubmission,
        Optional<MissionSubmissionInfo> submission
    ) {
        public static MyOriginalWorkbookMissionInfo of(OriginalWorkbookMission m, MissionSubmissionInfo submission) {
            return MyOriginalWorkbookMissionInfo.builder()
                .originalWorkbookMissionId(m.getId())
                .title(m.getTitle())
                .description(m.getDescription())
                .missionType(m.getMissionType())
                .isNecessary(m.isNecessary())
                .hasSubmission(submission != null)
                .submission(Optional.ofNullable(submission))
                .build();
        }
    }

    @Builder
    public record MissionSubmissionInfo(
        Long missionSubmissionId,
        MissionType submittedAsType,
        String submittedContent,
        Instant submittedAt,
        Instant lastEditedAt,
        SubmissionStatus status,
        boolean hasFeedback,
        List<MissionFeedbackInfo> feedbacks
    ) {
        public static MissionSubmissionInfo of(MissionSubmission s, SubmissionStatus status, List<MissionFeedbackInfo> feedbacks) {
            return MissionSubmissionInfo.builder()
                .missionSubmissionId(s.getId())
                .submittedAsType(s.getSubmittedAsType())
                .submittedContent(s.getContent())
                .submittedAt(s.getCreatedAt())
                .lastEditedAt(s.getUpdatedAt())
                .status(status)
                .hasFeedback(!feedbacks.isEmpty())
                .feedbacks(feedbacks)
                .build();
        }
    }

    @Builder
    public record MissionFeedbackInfo(
        Long missionFeedbackId,
        Long reviewerMemberId,
        String content,
        FeedbackResult feedbackResult
    ) {
        public static MissionFeedbackInfo of(MissionFeedback f) {
            return MissionFeedbackInfo.builder()
                .missionFeedbackId(f.getId())
                .reviewerMemberId(f.getReviewerMemberId())
                .content(f.getContent())
                .feedbackResult(f.getFeedbackResult())
                .build();
        }
    }
}
