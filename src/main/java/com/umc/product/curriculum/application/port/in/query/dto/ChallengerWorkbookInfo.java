package com.umc.product.curriculum.application.port.in.query.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.MissionType;

import lombok.Builder;

/**
 * 챌린저 워크북 상세 조회 결과 Info
 *
 * @param challengerWorkbookId PK
 * @param originalWorkbookId   소속 원본 워크북 ID
 * @param receivedStudyGroupId 배포받은 스터디 그룹 ID (nullable)
 * @param challengerId         소유 챌린저의 멤버 ID
 * @param isExcused            인정 처리 여부
 * @param excusedReason        인정 처리 사유 (nullable)
 * @param content              워크북 내용 (nullable)
 * @param isBestWorkbook       베스트 워크북 선정 여부
 * @param requiredMissionIds   필수 미션 ID 집합
 * @param submissions          미션 제출물 목록
 */
@Builder
public record ChallengerWorkbookInfo(
    Long challengerWorkbookId,
    Long originalWorkbookId,
    Long receivedStudyGroupId,
    Long challengerId,
    boolean isExcused,
    String excusedReason,
    String content,
    boolean isBestWorkbook,
    Set<Long> requiredMissionIds,
    List<MissionSubmissionInfo> submissions
) {

    /**
     * 미션 제출물 정보
     *
     * @param missionSubmissionId       PK
     * @param originalWorkbookMissionId 소속 원본 워크북 미션 ID
     * @param submittedAsType           제출 당시의 미션 유형
     * @param submittedContent          제출 내용 (nullable)
     * @param submittedAt               최초 제출 일시
     * @param lastEditedAt              마지막 수정 일시
     * @param hasFeedback               피드백 존재 여부
     * @param feedbacks                 피드백 목록
     */
    @Builder
    public record MissionSubmissionInfo(
        Long missionSubmissionId,
        Long originalWorkbookMissionId,
        MissionType submittedAsType,
        String submittedContent,
        Instant submittedAt,
        Instant lastEditedAt,
        boolean hasFeedback,
        List<MissionFeedbackInfo> feedbacks
    ) {
    }

    /**
     * 미션 피드백 정보
     *
     * @param missionFeedbackId PK
     * @param reviewerMemberId  피드백 작성자(운영진)의 멤버 ID
     * @param content           피드백 내용
     * @param feedbackResult    평가 결과 (PASS / FAIL)
     */
    @Builder
    public record MissionFeedbackInfo(
        Long missionFeedbackId,
        Long reviewerMemberId,
        String content,
        FeedbackResult feedbackResult
    ) {
    }
}
