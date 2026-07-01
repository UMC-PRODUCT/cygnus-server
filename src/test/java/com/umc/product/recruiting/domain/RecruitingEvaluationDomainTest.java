package com.umc.product.recruiting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewEvaluationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@DisplayName("Recruiting evaluation domain")
class RecruitingEvaluationDomainTest {

    @Test
    @DisplayName("기본 평가 템플릿은 기본 평가 기준을 생성한다")
    void 기본_평가_템플릿은_기본_평가_기준을_생성한다() {
        RecruitingEvaluationTemplate template = RecruitingEvaluationTemplate.createDefault(applicationForm());

        List<RecruitingEvaluationCriterion> criteria = RecruitingEvaluationCriterion.createDefaults(template);

        assertThat(criteria)
            .extracting(RecruitingEvaluationCriterion::getName)
            .containsExactly("역량", "협업", "성장 가능성");
    }

    @Test
    @DisplayName("지원 폼별로 커스텀 평가 기준을 만들 수 있다")
    void 지원_폼별로_커스텀_평가_기준을_만들_수_있다() {
        RecruitingEvaluationTemplate template = RecruitingEvaluationTemplate.createDefault(applicationForm());

        RecruitingEvaluationCriterion criterion = RecruitingEvaluationCriterion.create(
            template,
            "기술 이해도",
            "지원 트랙에 필요한 기술 이해도",
            1,
            5,
            1,
            true
        );

        assertThat(criterion.getTemplate()).isSameAs(template);
        assertThat(criterion.getName()).isEqualTo("기술 이해도");
        assertThat(criterion.getScoreMin()).isEqualTo(1);
        assertThat(criterion.getScoreMax()).isEqualTo(5);
    }

    @Test
    @DisplayName("면접관 평가는 draft에서 submit으로 전환된다")
    void 면접관_평가는_draft에서_submit으로_전환된다() {
        RecruitingInterviewEvaluation evaluation = RecruitingInterviewEvaluation.createDraft(assignment(), 20L);

        evaluation.submit(4, "충분히 준비된 지원자");

        assertThat(evaluation.getStatus()).isEqualTo(RecruitingInterviewEvaluationStatus.SUBMITTED);
        assertThat(evaluation.getScore()).isEqualTo(4);
        assertThat(evaluation.getComment()).isEqualTo("충분히 준비된 지원자");
        assertThat(evaluation.getSubmittedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 제출한 면접관 평가는 다시 제출할 수 없다")
    void 이미_제출한_면접관_평가는_다시_제출할_수_없다() {
        RecruitingInterviewEvaluation evaluation = RecruitingInterviewEvaluation.createDraft(assignment(), 20L);
        evaluation.submit(4, "1차 제출");

        assertThatThrownBy(() -> evaluation.submit(5, "2차 제출"))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_EVALUATION_INVALID_TRANSITION);
    }

    @Test
    @DisplayName("본인 평가 제출 전에는 타 면접관 평가를 조회할 수 없다")
    void 본인_평가_제출_전에는_타_면접관_평가를_조회할_수_없다() {
        RecruitingInterviewEvaluation draft = RecruitingInterviewEvaluation.createDraft(assignment(), 20L);

        assertThat(RecruitingInterviewEvaluationVisibility.canReadPeerEvaluations(draft)).isFalse();

        draft.submit(4, "제출 완료");

        assertThat(RecruitingInterviewEvaluationVisibility.canReadPeerEvaluations(draft)).isTrue();
    }

    @Test
    @DisplayName("같은 지원서에 같은 면접관의 제출 평가는 중복될 수 없다")
    void 같은_지원서에_같은_면접관의_제출_평가는_중복될_수_없다() {
        RecruitingInterviewAssignment assignment = assignment();
        RecruitingInterviewEvaluation submitted = RecruitingInterviewEvaluation.createDraft(assignment, 20L);
        submitted.submit(4, "제출 완료");
        RecruitingInterviewEvaluation candidate = RecruitingInterviewEvaluation.createDraft(assignment, 20L);

        assertThatThrownBy(() -> candidate.validateNoSubmittedDuplicate(List.of(submitted)))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_EVALUATION_ALREADY_SUBMITTED);
    }

    private RecruitingInterviewAssignment assignment() {
        return RecruitingInterviewAssignment.assign(
            application(),
            20L,
            Instant.parse("2026-07-10T01:00:00Z"),
            Instant.parse("2026-07-10T01:30:00Z"),
            "온라인"
        );
    }

    private RecruitingApplication application() {
        RecruitingApplication application = RecruitingApplication.createDraft(
            applicationForm(),
            200L,
            1L,
            "survey-identity-1",
            "R-0001",
            "a***@example.com"
        );
        application.submit(1L);
        application.passDocument(2L, "서류 합격");
        return application;
    }

    private RecruitingApplicationForm applicationForm() {
        return RecruitingApplicationForm.create(
            RecruitingRound.createRegular(RecruitingSeason.create(9L, 1L)),
            100L,
            ChallengerTrack.WEB_PRODUCT_ENGINEER
        );
    }
}
