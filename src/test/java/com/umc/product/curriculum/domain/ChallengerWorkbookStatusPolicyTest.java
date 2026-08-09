package com.umc.product.curriculum.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.umc.product.curriculum.domain.enums.ChallengerWorkbookStatus;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.SubmissionStatus;

class ChallengerWorkbookStatusPolicyTest {

    @Nested
    @DisplayName("제출물 상태")
    class SubmissionStatusTest {

        @Test
        @DisplayName("피드백이 없으면 평가 대기다")
        void noFeedback_pending() {
            assertThat(ChallengerWorkbookStatusPolicy.resolveSubmissionStatus(List.of()))
                .isEqualTo(SubmissionStatus.PENDING);
        }

        @Test
        @DisplayName("PASS 피드백만 있으면 통과다")
        void onlyPass_pass() {
            assertThat(ChallengerWorkbookStatusPolicy.resolveSubmissionStatus(List.of(FeedbackResult.PASS)))
                .isEqualTo(SubmissionStatus.PASS);
        }

        @Test
        @DisplayName("FAIL 피드백만 있으면 탈락이다")
        void onlyFail_fail() {
            assertThat(ChallengerWorkbookStatusPolicy.resolveSubmissionStatus(List.of(FeedbackResult.FAIL)))
                .isEqualTo(SubmissionStatus.FAIL);
        }

        @Test
        @DisplayName("FAIL 피드백이 하나라도 있으면 PASS 가 섞여 있어도 탈락이다")
        void mixedFeedback_fail() {
            assertThat(ChallengerWorkbookStatusPolicy.resolveSubmissionStatus(
                List.of(FeedbackResult.FAIL, FeedbackResult.PASS)
            )).isEqualTo(SubmissionStatus.FAIL);
        }
    }

    @Nested
    @DisplayName("워크북 상태")
    class WorkbookStatusTest {

        @Test
        @DisplayName("인정 처리되면 제출물이 없어도 통과다")
        void excused_pass() {
            assertThat(ChallengerWorkbookStatusPolicy.resolveWorkbookStatus(true, Map.of(), Set.of(1L)))
                .isEqualTo(ChallengerWorkbookStatus.PASS);
        }

        @Test
        @DisplayName("제출물이 하나도 없으면 진행 중이다")
        void noSubmission_inProgress() {
            assertThat(ChallengerWorkbookStatusPolicy.resolveWorkbookStatus(false, Map.of(), Set.of()))
                .isEqualTo(ChallengerWorkbookStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("탈락한 제출물이 있으면 워크북도 탈락이다")
        void anyFail_fail() {
            assertThat(ChallengerWorkbookStatusPolicy.resolveWorkbookStatus(
                false,
                Map.of(1L, SubmissionStatus.PASS, 2L, SubmissionStatus.FAIL),
                Set.of(1L, 2L)
            )).isEqualTo(ChallengerWorkbookStatus.FAIL);
        }

        @Test
        @DisplayName("필수 미션이 전부 통과되면 워크북도 통과다")
        void allRequiredPassed_pass() {
            assertThat(ChallengerWorkbookStatusPolicy.resolveWorkbookStatus(
                false,
                Map.of(1L, SubmissionStatus.PASS, 2L, SubmissionStatus.PASS),
                Set.of(1L, 2L)
            )).isEqualTo(ChallengerWorkbookStatus.PASS);
        }

        @Test
        @DisplayName("통과한 제출물만 있어도 필수 미션이 남아 있으면 진행 중이다")
        void requiredMissionMissing_inProgress() {
            assertThat(ChallengerWorkbookStatusPolicy.resolveWorkbookStatus(
                false,
                Map.of(1L, SubmissionStatus.PASS),
                Set.of(1L, 2L)
            )).isEqualTo(ChallengerWorkbookStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("평가 대기 중인 제출물이 섞여 있으면 진행 중이다")
        void pendingSubmission_inProgress() {
            assertThat(ChallengerWorkbookStatusPolicy.resolveWorkbookStatus(
                false,
                Map.of(1L, SubmissionStatus.PASS, 2L, SubmissionStatus.PENDING),
                Set.of(1L, 2L)
            )).isEqualTo(ChallengerWorkbookStatus.IN_PROGRESS);
        }
    }
}
