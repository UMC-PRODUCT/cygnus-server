package com.umc.product.curriculum.domain;

import java.util.Objects;

import com.umc.product.common.BaseEntity;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mission_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_submission_id", nullable = false)
    private MissionSubmission missionSubmission;

    @Column(nullable = false)
    private Long reviewerMemberId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackResult feedbackResult;

    @Builder(access = AccessLevel.PRIVATE)
    private MissionFeedback(
        MissionSubmission missionSubmission,
        Long reviewerMemberId,
        String content,
        FeedbackResult feedbackResult
    ) {
        this.missionSubmission = missionSubmission;
        this.reviewerMemberId = reviewerMemberId;
        this.content = content;
        this.feedbackResult = feedbackResult;
    }

    public static MissionFeedback create(
        MissionSubmission missionSubmission,
        Long reviewerMemberId,
        String content,
        FeedbackResult feedbackResult
    ) {
        validateContent(content);
        if (feedbackResult == null) {
            throw new CurriculumDomainException(CurriculumErrorCode.FEEDBACK_RESULT_REQUIRED);
        }
        if (missionSubmission.isWithdrawn()) {
            throw new CurriculumDomainException(CurriculumErrorCode.MISSION_SUBMISSION_ALREADY_WITHDRAWN);
        }

        return MissionFeedback.builder()
            .missionSubmission(missionSubmission)
            .reviewerMemberId(reviewerMemberId)
            .content(content)
            .feedbackResult(feedbackResult)
            .build();
    }

    public void edit(String content) {
        validateContent(content);
        this.content = content;
    }

    public boolean isReviewedBy(Long memberId) {
        return Objects.equals(this.reviewerMemberId, memberId);
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new CurriculumDomainException(CurriculumErrorCode.FEEDBACK_REQUIRED);
        }
    }
}
