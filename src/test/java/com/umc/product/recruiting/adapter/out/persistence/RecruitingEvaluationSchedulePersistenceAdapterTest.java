package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.umc.product.recruiting.domain.RecruitingApplicationEvaluation;
import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.PersistenceException;

@PersistenceAdapterTest
@Import({
    RecruitingSeasonPersistenceAdapter.class,
    RecruitingRoundPersistenceAdapter.class,
    RecruitingApplicationFormPersistenceAdapter.class,
    RecruitingApplicationPersistenceAdapter.class,
    RecruitingApplicationQueryRepository.class,
    RecruitingApplicationEvaluationPersistenceAdapter.class,
    RecruitingInterviewSchedulePersistenceAdapter.class,
    RecruitingSubmittedInterviewEvaluationPersistenceAdapter.class
})
class RecruitingEvaluationSchedulePersistenceAdapterTest extends RecruitingPersistenceAdapterTestSupport {

    @Autowired
    RecruitingApplicationEvaluationPersistenceAdapter evaluationAdapter;

    @Autowired
    RecruitingInterviewSchedulePersistenceAdapter scheduleAdapter;

    @Autowired
    RecruitingSubmittedInterviewEvaluationPersistenceAdapter submittedEvaluationAdapter;

    @Test
    @DisplayName("단계별 평가와 확정 면접 일정을 저장하고 application은 지연 로딩한다")
    void 단계별_평가와_확정_면접_일정을_저장하고_application은_지연_로딩한다() {
        RecruitingGraph graph = persistApplicationGraph(
            3L,
            30L,
            1,
            "evaluation:schedule",
            RecruitingApplicationStatus.DOCUMENT_PASSED
        );
        RecruitingApplicationEvaluation document = RecruitingApplicationEvaluation.createDraft(
            graph.application(),
            901L,
            RecruitingEvaluatorStage.DOCUMENT
        );
        evaluationAdapter.saveEvaluation(document);
        RecruitingApplicationEvaluation interview = RecruitingApplicationEvaluation.createDraft(
            graph.application(),
            901L,
            RecruitingEvaluatorStage.INTERVIEW
        );
        interview.submit(RecruitingApplicationEvaluationDecision.REJECTED, "추가 논의");
        evaluationAdapter.saveEvaluation(interview);
        RecruitingInterviewSchedule schedule = RecruitingInterviewSchedule.requestAvailability(
            graph.application(),
            "카카오톡 @umc"
        );
        schedule.submitAvailability(700L);
        schedule.confirm(
            Instant.parse("2026-08-12T01:00:00Z"),
            Instant.parse("2026-08-12T01:30:00Z"),
            "온라인",
            "카카오톡 @umc"
        );
        scheduleAdapter.saveSchedule(schedule);
        em.flush();
        em.clear();

        RecruitingApplicationEvaluation reloaded = evaluationAdapter
            .listByApplicationIdAndStage(graph.application().getId(), RecruitingEvaluatorStage.INTERVIEW)
            .getFirst();
        RecruitingInterviewSchedule reloadedSchedule = scheduleAdapter
            .getByApplicationId(graph.application().getId());

        assertThat(reloaded.getDecision()).isEqualTo(RecruitingApplicationEvaluationDecision.REJECTED);
        assertThat(reloadedSchedule.getLocation()).isEqualTo("온라인");
        assertThat(em.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil()
            .isLoaded(reloaded.getApplication())).isFalse();
        assertThat(em.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil()
            .isLoaded(reloadedSchedule.getApplication())).isFalse();
        assertThat(submittedEvaluationAdapter.existsSubmittedByRoundId(graph.round().getId())).isTrue();
        assertThat(submittedEvaluationAdapter.existsSubmittedByApplicationId(graph.application().getId())).isTrue();
    }

    @Test
    @DisplayName("같은 지원서 평가자 단계 평가는 데이터베이스에서 중복 저장할 수 없다")
    void 같은_지원서_평가자_단계_평가는_데이터베이스에서_중복_저장할_수_없다() {
        RecruitingGraph graph = persistApplicationGraph(
            4L,
            40L,
            1,
            "evaluation:unique",
            RecruitingApplicationStatus.SUBMITTED
        );
        evaluationAdapter.saveEvaluation(RecruitingApplicationEvaluation.createDraft(
            graph.application(),
            901L,
            RecruitingEvaluatorStage.DOCUMENT
        ));
        em.flush();

        assertThatThrownBy(() -> {
            evaluationAdapter.saveEvaluation(RecruitingApplicationEvaluation.createDraft(
                graph.application(),
                901L,
                RecruitingEvaluatorStage.DOCUMENT
            ));
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("서류 평가 제출만으로는 Task6 면접 질문을 동결하지 않는다")
    void 서류_평가_제출만으로는_Task6_면접_질문을_동결하지_않는다() {
        RecruitingGraph graph = persistApplicationGraph(
            8L,
            80L,
            1,
            "evaluation:document-only",
            RecruitingApplicationStatus.SUBMITTED
        );
        RecruitingApplicationEvaluation evaluation = RecruitingApplicationEvaluation.createDraft(
            graph.application(),
            901L,
            RecruitingEvaluatorStage.DOCUMENT
        );
        evaluation.submit(RecruitingApplicationEvaluationDecision.APPROVED, "서류 평가");
        evaluationAdapter.saveEvaluation(evaluation);
        em.flush();
        em.clear();

        assertThat(submittedEvaluationAdapter.existsSubmittedByRoundId(graph.round().getId())).isFalse();
        assertThat(submittedEvaluationAdapter.existsSubmittedByApplicationId(graph.application().getId())).isFalse();
    }

    @Test
    @DisplayName("제출 상태에서 decision이 없는 평가는 데이터베이스가 거부한다")
    void 제출_상태에서_decision이_없는_평가는_데이터베이스가_거부한다() {
        RecruitingGraph graph = persistApplicationGraph(
            5L,
            50L,
            1,
            "evaluation:check",
            RecruitingApplicationStatus.SUBMITTED
        );
        em.flush();

        assertThatThrownBy(() -> em.getEntityManager().createNativeQuery("""
            INSERT INTO recruiting_application_evaluation (
                created_at, updated_at, recruiting_application_id, evaluator_member_id,
                stage, status, decision, submitted_at
            ) VALUES (
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :applicationId, 901,
                'DOCUMENT', 'SUBMITTED', NULL, CURRENT_TIMESTAMP
            )
            """)
            .setParameter("applicationId", graph.application().getId())
            .executeUpdate())
            .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("지원서별 면접 일정은 하나만 저장할 수 있다")
    void 지원서별_면접_일정은_하나만_저장할_수_있다() {
        RecruitingGraph graph = persistApplicationGraph(
            6L,
            60L,
            1,
            "schedule:unique",
            RecruitingApplicationStatus.DOCUMENT_PASSED
        );
        scheduleAdapter.saveSchedule(RecruitingInterviewSchedule.requestAvailability(
            graph.application(),
            "카카오톡 @umc"
        ));
        em.flush();

        assertThatThrownBy(() -> {
            scheduleAdapter.saveSchedule(RecruitingInterviewSchedule.requestAvailability(
                graph.application(),
                "이메일"
            ));
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("확정 면접의 잘못된 시간 순서는 데이터베이스가 거부한다")
    void 확정_면접의_잘못된_시간_순서는_데이터베이스가_거부한다() {
        RecruitingGraph graph = persistApplicationGraph(
            7L,
            70L,
            1,
            "schedule:check",
            RecruitingApplicationStatus.DOCUMENT_PASSED
        );
        em.flush();

        assertThatThrownBy(() -> em.getEntityManager().createNativeQuery("""
            INSERT INTO recruiting_interview_schedule (
                created_at, updated_at, recruiting_application_id,
                availability_form_response_id, status, starts_at, ends_at, location, contact_snapshot,
                request_mail_status, request_mail_attempts,
                confirmation_mail_status, confirmation_mail_attempts
            ) VALUES (
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :applicationId,
                700, 'CONFIRMED',
                TIMESTAMP WITH TIME ZONE '2026-08-12 02:00:00+00',
                TIMESTAMP WITH TIME ZONE '2026-08-12 01:00:00+00',
                '온라인', '카카오톡 @umc', 'PENDING', 0, 'PENDING', 0
            )
            """)
            .setParameter("applicationId", graph.application().getId())
            .executeUpdate())
            .isInstanceOf(PersistenceException.class);
    }
}
