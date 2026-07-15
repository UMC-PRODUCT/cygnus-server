package com.umc.product.recruiting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@DisplayName("Recruiting 지원서 평가 도메인")
class RecruitingEvaluationDomainTest {

    @Test
    @DisplayName("평가 초안은 결정 없이 저장할 수 있다")
    void 평가_초안은_결정_없이_저장할_수_있다() {
        RecruitingApplicationEvaluation evaluation = RecruitingApplicationEvaluation.createDraft(
            application(),
            20L,
            RecruitingEvaluatorStage.DOCUMENT
        );

        evaluation.updateDraft(null, "검토 중");

        assertThat(evaluation.getStatus()).isEqualTo(RecruitingApplicationEvaluationStatus.DRAFT);
        assertThat(evaluation.getDecision()).isNull();
        assertThat(evaluation.getComment()).isEqualTo("검토 중");
    }

    @Test
    @DisplayName("평가 제출에는 합격 결정이 필요하다")
    void 평가_제출에는_합격_결정이_필요하다() {
        RecruitingApplicationEvaluation evaluation = RecruitingApplicationEvaluation.createDraft(
            application(),
            20L,
            RecruitingEvaluatorStage.INTERVIEW
        );

        assertThatThrownBy(() -> evaluation.submit(null, "결정 없음"))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_EVALUATION_DECISION_REQUIRED);
    }

    @Test
    @DisplayName("평가는 APPROVED 또는 REJECTED 결정을 제출할 수 있다")
    void 평가는_APPROVED_또는_REJECTED_결정을_제출할_수_있다() {
        assertThat(RecruitingApplicationEvaluationDecision.values())
            .containsExactly(
                RecruitingApplicationEvaluationDecision.APPROVED,
                RecruitingApplicationEvaluationDecision.REJECTED
            );

        for (RecruitingApplicationEvaluationDecision decision : RecruitingApplicationEvaluationDecision.values()) {
            RecruitingApplicationEvaluation evaluation = RecruitingApplicationEvaluation.createDraft(
                application(),
                20L,
                RecruitingEvaluatorStage.INTERVIEW
            );

            evaluation.submit(decision, "제출 완료");

            assertThat(evaluation.getStatus()).isEqualTo(RecruitingApplicationEvaluationStatus.SUBMITTED);
            assertThat(evaluation.getDecision()).isEqualTo(decision);
            assertThat(evaluation.getSubmittedAt()).isNotNull();
        }
    }

    @Test
    @DisplayName("제출한 평가는 수정할 수 없다")
    void 제출한_평가는_수정할_수_없다() {
        RecruitingApplicationEvaluation evaluation = RecruitingApplicationEvaluation.createDraft(
            application(),
            20L,
            RecruitingEvaluatorStage.DOCUMENT
        );
        evaluation.submit(RecruitingApplicationEvaluationDecision.APPROVED, "제출 완료");

        assertThatThrownBy(() -> evaluation.updateDraft(RecruitingApplicationEvaluationDecision.REJECTED, "변경"))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_EVALUATION_INVALID_TRANSITION);
    }

    @Test
    @DisplayName("평가 의견은 2000자를 초과할 수 없다")
    void 평가_의견은_2000자를_초과할_수_없다() {
        RecruitingApplicationEvaluation evaluation = RecruitingApplicationEvaluation.createDraft(
            application(),
            20L,
            RecruitingEvaluatorStage.DOCUMENT
        );

        assertThatThrownBy(() -> evaluation.updateDraft(null, "a".repeat(2001)))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_EVALUATION_COMMENT_TOO_LONG);
    }

    @Test
    @DisplayName("평가 의견은 2000자까지 저장할 수 있다")
    void 평가_의견은_2000자까지_저장할_수_있다() {
        RecruitingApplicationEvaluation evaluation = RecruitingApplicationEvaluation.createDraft(
            application(),
            20L,
            RecruitingEvaluatorStage.DOCUMENT
        );

        evaluation.updateDraft(null, "a".repeat(2000));

        assertThat(evaluation.getComment()).hasSize(2000);
    }

    private RecruitingApplication application() {
        RecruitingApplicationForm form = applicationForm();
        return RecruitingApplication.createMemberDraft(
            form,
            200L,
            1L,
            RecruitingApplicantProfile.create(
                form.getRound(),
                "지원자",
                RecruitingApplicantEmail.from("applicant@example.com"),
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                null
            ),
            "A1B2C3"
        );
    }

    private RecruitingApplicationForm applicationForm() {
        RecruitingRound round = RecruitingRound.createRegular(
            RecruitingSeason.create(9L, 1L),
            RecruitingRoundConfiguration.of(
                List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER),
                false,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-08T00:00:00Z"),
                Instant.parse("2026-08-10T00:00:00Z"),
                true,
                Instant.parse("2026-08-11T00:00:00Z"),
                Instant.parse("2026-08-15T00:00:00Z"),
                Instant.parse("2026-08-16T00:00:00Z"),
                300L,
                null,
                "문의 채널"
            )
        );
        return RecruitingApplicationForm.create(round, 100L);
    }
}
