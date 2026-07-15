package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.service.command.RecruitingInterviewQuestionMutationPolicy;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationEvaluation;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingApplicationInterviewQuestion;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundEvaluator;
import com.umc.product.recruiting.domain.RecruitingRoundInterviewQuestion;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.PersistenceException;

@PersistenceAdapterTest
@Import({
    RecruitingRoundEvaluatorPersistenceAdapter.class,
    RecruitingRoundInterviewQuestionPersistenceAdapter.class,
    RecruitingApplicationInterviewQuestionPersistenceAdapter.class,
    RecruitingSubmittedInterviewEvaluationPersistenceAdapter.class,
    RecruitingInterviewQuestionMutationPolicy.class,
})
class RecruitingEvaluatorQuestionPersistenceAdapterTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    RecruitingRoundEvaluatorPersistenceAdapter evaluatorAdapter;

    @Autowired
    RecruitingRoundInterviewQuestionPersistenceAdapter roundQuestionAdapter;

    @Autowired
    RecruitingApplicationInterviewQuestionPersistenceAdapter applicationQuestionAdapter;

    @Autowired
    RecruitingSubmittedInterviewEvaluationPersistenceAdapter submittedEvaluationAdapter;

    @Autowired
    RecruitingInterviewQuestionMutationPolicy mutationPolicy;

    @Test
    @DisplayName("차수 평가자는 서류와 면접 전형의 공통 whitelist로 조회된다")
    void evaluatorIsScopedByRound() {
        RecruitingRound round = persistRound();
        evaluatorAdapter.save(RecruitingRoundEvaluator.create(round, 10L));
        em.flush();
        em.clear();

        List<RecruitingRoundEvaluator> evaluators = evaluatorAdapter.listByRoundId(round.getId());
        assertThat(evaluators)
            .extracting(RecruitingRoundEvaluator::getMemberId)
            .containsExactly(10L);
        assertThat(em.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil()
            .isLoaded(evaluators.getFirst().getRound())).isFalse();
    }

    @Test
    @DisplayName("같은 차수의 같은 회원은 데이터베이스에서 중복 평가자로 저장할 수 없다")
    void evaluatorIsUniqueByRoundAndMember() {
        RecruitingRound round = persistRound();
        evaluatorAdapter.save(RecruitingRoundEvaluator.create(round, 10L));
        em.flush();

        assertThatThrownBy(() -> {
            evaluatorAdapter.save(RecruitingRoundEvaluator.create(round, 10L));
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("공통 질문은 순서대로 조회하고 비활성 질문을 활성 목록에서 제외한다")
    void listActiveRoundQuestionsInOrder() {
        RecruitingRound round = persistRound();
        RecruitingRoundInterviewQuestion later = roundQuestionAdapter.save(
            RecruitingRoundInterviewQuestion.create(round, "두 번째", 1, 10L)
        );
        roundQuestionAdapter.save(RecruitingRoundInterviewQuestion.create(round, "첫 번째", 0, 20L));
        later.deactivateBeforeFirstEvaluationSubmission(30L);
        roundQuestionAdapter.save(later);
        em.flush();
        em.clear();

        List<RecruitingRoundInterviewQuestion> allQuestions = roundQuestionAdapter.listByRoundId(round.getId());
        assertThat(allQuestions)
            .extracting(RecruitingRoundInterviewQuestion::getContent)
            .containsExactly("첫 번째", "두 번째");
        assertThat(roundQuestionAdapter.listActiveByRoundId(round.getId()))
            .extracting(RecruitingRoundInterviewQuestion::getContent)
            .containsExactly("첫 번째");
        assertThat(em.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil()
            .isLoaded(allQuestions.getFirst().getRound())).isFalse();
    }

    @Test
    @DisplayName("공통 질문의 생성자와 최종 변경자를 데이터베이스에 보존한다")
    void persistRoundQuestionCreatorAndLastModifier() {
        RecruitingRound round = persistRound();
        RecruitingRoundInterviewQuestion question = roundQuestionAdapter.save(
            RecruitingRoundInterviewQuestion.create(round, "공통 질문", 0, 10L)
        );
        question.updateBeforeFirstEvaluationSubmission("수정 질문", 1, 20L);
        roundQuestionAdapter.save(question);
        em.flush();
        em.clear();

        RecruitingRoundInterviewQuestion reloaded = roundQuestionAdapter.getById(question.getId());

        assertThat(reloaded.getCreatorMemberId()).isEqualTo(10L);
        assertThat(reloaded.getLastModifiedByMemberId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("공통 질문 내용 데이터베이스 제약은 공백을 거부한다")
    void roundQuestionContentCheckRejectsBlank() {
        RecruitingRound round = persistRound();
        em.flush();

        assertThatThrownBy(() -> em.getEntityManager().createNativeQuery("""
            INSERT INTO recruiting_round_interview_question
                (created_at, updated_at, recruiting_round_id, content, order_no, active,
                 creator_member_id, last_modified_by_member_id)
            VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :roundId, '   ', 0, TRUE, 10, 10)
            """)
            .setParameter("roundId", round.getId())
            .executeUpdate())
            .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("개별 질문은 지원서를 지연 로딩으로 참조하고 활성 질문만 순서대로 조회한다")
    void applicationQuestionUsesLazyApplicationReference() {
        RecruitingApplication application = persistApplication();
        applicationQuestionAdapter.save(RecruitingApplicationInterviewQuestion.create(application, "두 번째", 1));
        applicationQuestionAdapter.save(RecruitingApplicationInterviewQuestion.create(application, "첫 번째", 0));
        em.flush();
        em.clear();

        List<RecruitingApplicationInterviewQuestion> questions =
            applicationQuestionAdapter.listActiveByApplicationId(application.getId());

        assertThat(questions)
            .extracting(RecruitingApplicationInterviewQuestion::getContent)
            .containsExactly("첫 번째", "두 번째");
        assertThat(em.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil()
            .isLoaded(questions.getFirst().getApplication())).isFalse();
    }

    @Test
    @DisplayName("개별 질문 순서 데이터베이스 제약은 음수를 거부한다")
    void applicationQuestionOrderCheckRejectsNegativeValue() {
        RecruitingApplication application = persistApplication();
        em.flush();

        assertThatThrownBy(() -> em.getEntityManager().createNativeQuery("""
            INSERT INTO recruiting_application_interview_question
                (created_at, updated_at, recruiting_application_id, content, order_no, active)
            VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :applicationId, '질문', -1, TRUE)
            """)
            .setParameter("applicationId", application.getId())
            .executeUpdate())
            .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("제출된 신규 면접 평가는 질문 동결 port에서 차수와 지원서 단위로 조회된다")
    void submittedEvaluationLocksRoundAndApplicationQuestions() {
        RecruitingApplication application = persistApplication();
        RecruitingRoundInterviewQuestion roundQuestion = em.persist(
            RecruitingRoundInterviewQuestion.create(application.getRound(), "공통 질문", 0, 10L)
        );
        RecruitingApplicationInterviewQuestion applicationQuestion = em.persist(
            RecruitingApplicationInterviewQuestion.create(application, "개별 질문", 0)
        );
        RecruitingApplicationEvaluation evaluation = RecruitingApplicationEvaluation.createDraft(
            application,
            10L,
            RecruitingEvaluatorStage.INTERVIEW
        );
        evaluation.submit(RecruitingApplicationEvaluationDecision.APPROVED, "제출 완료");
        em.persist(evaluation);
        em.flush();
        em.clear();

        assertThat(submittedEvaluationAdapter.existsSubmittedByRoundId(application.getRound().getId())).isTrue();
        assertThat(submittedEvaluationAdapter.existsSubmittedByApplicationId(application.getId())).isTrue();
        assertThatThrownBy(() -> mutationPolicy.assertMutable(roundQuestion))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_IMMUTABLE);
        assertThatThrownBy(() -> mutationPolicy.assertMutable(applicationQuestion))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_IMMUTABLE);
    }

    private RecruitingRound persistRound() {
        RecruitingSeason season = em.persist(RecruitingSeason.create(1L, 10L));
        RecruitingRound round = RecruitingRound.createRegular(season);
        ReflectionTestUtils.setField(
            round,
            "recruitableTracks",
            List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER)
        );
        return em.persist(round);
    }

    private RecruitingApplication persistApplication() {
        RecruitingRound round = persistRound();
        RecruitingApplicationForm form = em.persist(RecruitingApplicationForm.create(round, 100L));
        return em.persist(RecruitingApplication.createMemberDraft(
            form,
            200L,
            300L,
            RecruitingApplicantProfile.create(
                round,
                "지원자",
                RecruitingApplicantEmail.from("applicant@example.com"),
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                null
            ),
            "A1B2C3"
        ));
    }
}
