package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.SaveRecruitingApplicationEvaluationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationEvaluationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundEvaluatorPort;
import com.umc.product.recruiting.application.service.command.RecruitingApplicationEvaluationCommandService;
import com.umc.product.recruiting.application.service.command.RecruitingApplicationInterviewQuestionCommandService;
import com.umc.product.recruiting.application.service.command.RecruitingConcurrencyLockService;
import com.umc.product.recruiting.application.service.command.RecruitingInterviewQuestionMutationPolicy;
import com.umc.product.recruiting.application.service.command.RecruitingRoundInterviewQuestionCommandService;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationEvaluation;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingApplicationInterviewQuestion;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingRoundInterviewQuestion;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
    RecruitingSeasonPersistenceAdapter.class,
    RecruitingRoundPersistenceAdapter.class,
    RecruitingApplicationFormPersistenceAdapter.class,
    RecruitingApplicationPersistenceAdapter.class,
    RecruitingApplicationQueryRepository.class,
    RecruitingApplicantLockPersistenceAdapter.class,
    RecruitingApplicationEvaluationPersistenceAdapter.class,
    RecruitingSubmittedInterviewEvaluationPersistenceAdapter.class,
    RecruitingRoundInterviewQuestionPersistenceAdapter.class,
    RecruitingApplicationInterviewQuestionPersistenceAdapter.class,
    RecruitingConcurrencyLockService.class,
    RecruitingInterviewQuestionMutationPolicy.class,
    RecruitingApplicationEvaluationCommandService.class,
    RecruitingRoundInterviewQuestionCommandService.class,
    RecruitingApplicationInterviewQuestionCommandService.class
})
class RecruitingEvaluationQuestionConcurrencyTest {

    private static final Long EVALUATOR_MEMBER_ID = 501L;
    private static final Long MANAGER_MEMBER_ID = 502L;
    private static final AtomicLong FIXTURE_SEQUENCE = new AtomicLong(300L);

    @Autowired
    RecruitingSeasonPersistenceAdapter seasonAdapter;
    @MockitoSpyBean
    RecruitingRoundPersistenceAdapter roundAdapter;
    @Autowired
    RecruitingApplicationFormPersistenceAdapter formAdapter;
    @Autowired
    RecruitingApplicationPersistenceAdapter applicationAdapter;
    @Autowired
    RecruitingApplicationEvaluationPersistenceAdapter evaluationAdapter;
    @Autowired
    RecruitingRoundInterviewQuestionPersistenceAdapter roundQuestionAdapter;
    @Autowired
    RecruitingApplicationInterviewQuestionPersistenceAdapter applicationQuestionAdapter;
    @Autowired
    RecruitingApplicationEvaluationCommandService evaluationCommandService;
    @Autowired
    RecruitingRoundInterviewQuestionCommandService roundQuestionCommandService;
    @Autowired
    RecruitingApplicationInterviewQuestionCommandService applicationQuestionCommandService;
    @Autowired
    PlatformTransactionManager transactionManager;

    @MockitoBean
    GetRecruitingRoundEvaluatorUseCase getRoundEvaluatorUseCase;
    @MockitoBean
    AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;
    @MockitoBean
    LoadRecruitingRoundEvaluatorPort loadRoundEvaluatorPort;

    @Test
    @DisplayName("평가 제출이 round lock을 먼저 잡으면 동시 평가 초안 저장은 제출 상태를 덮지 못한다")
    void submittedEvaluationCannotBeOverwrittenByConcurrentDraft() throws Exception {
        Fixture fixture = persistFixture();
        given(getRoundEvaluatorUseCase.canEvaluate(
            fixture.roundId(),
            EVALUATOR_MEMBER_ID
        )).willReturn(true);

        RacingResult result = raceAfterSubmitLocksRound(
            fixture,
            () -> saveEvaluationDraft(fixture.applicationId())
        );

        assertThat(result.submitSucceeded()).isTrue();
        assertThat(result.competingError()).isEqualTo(RecruitingErrorCode.RECRUITING_EVALUATION_INVALID_TRANSITION);
        assertThat(evaluationStatus(fixture.applicationId()))
            .isEqualTo(RecruitingApplicationEvaluationStatus.SUBMITTED);
    }

    @Test
    @DisplayName("평가 제출이 round lock을 먼저 잡으면 동시 공통 질문 수정을 거부한다")
    void submittedEvaluationFreezesConcurrentRoundQuestionMutation() throws Exception {
        Fixture fixture = persistFixture();
        given(getRoundEvaluatorUseCase.canEvaluate(
            fixture.roundId(),
            EVALUATOR_MEMBER_ID
        )).willReturn(true);

        RacingResult result = raceAfterSubmitLocksRound(
            fixture,
            () -> updateRoundQuestion(fixture)
        );

        assertThat(result.submitSucceeded()).isTrue();
        assertThat(result.competingError()).isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_IMMUTABLE);
        assertThat(roundQuestionContent(fixture.roundQuestionId())).isEqualTo("기존 공통 질문");
    }

    @Test
    @DisplayName("평가 제출이 round와 application lock을 먼저 잡으면 동시 개별 질문 수정을 거부한다")
    void submittedEvaluationFreezesConcurrentApplicationQuestionMutation() throws Exception {
        Fixture fixture = persistFixture();
        given(getRoundEvaluatorUseCase.canEvaluate(
            fixture.roundId(),
            EVALUATOR_MEMBER_ID
        )).willReturn(true);
        given(loadRoundEvaluatorPort.existsByRoundIdAndMemberId(
            fixture.roundId(),
            EVALUATOR_MEMBER_ID
        )).willReturn(true);

        RacingResult result = raceAfterSubmitLocksRound(
            fixture,
            () -> updateApplicationQuestion(fixture)
        );

        assertThat(result.submitSucceeded()).isTrue();
        assertThat(result.competingError()).isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_IMMUTABLE);
        assertThat(applicationQuestionContent(fixture.applicationQuestionId())).isEqualTo("기존 개별 질문");
    }

    private RacingResult raceAfterSubmitLocksRound(Fixture fixture, CompetingAction competingAction)
        throws Exception {
        CountDownLatch submitHasRoundLock = new CountDownLatch(1);
        CountDownLatch allowSubmit = new CountDownLatch(1);
        doAnswer(invocation -> {
            RecruitingRound round = (RecruitingRound) invocation.callRealMethod();
            if (Thread.currentThread().getName().equals("evaluation-submit")) {
                submitHasRoundLock.countDown();
                allowSubmit.await();
            }
            return round;
        }).when(roundAdapter).getByIdForUpdate(fixture.roundId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> submitFuture = executor.submit(() -> {
                Thread.currentThread().setName("evaluation-submit");
                submitEvaluation(fixture.applicationId());
                return true;
            });
            submitHasRoundLock.await();
            Future<RecruitingErrorCode> competingFuture = executor.submit(() -> {
                Thread.currentThread().setName("competing-mutation");
                try {
                    competingAction.run();
                    return null;
                } catch (RecruitingDomainException exception) {
                    return (RecruitingErrorCode) exception.getBaseCode();
                }
            });
            assertThat(competingFuture.isDone()).isFalse();
            allowSubmit.countDown();
            return new RacingResult(submitFuture.get(), competingFuture.get());
        } finally {
            allowSubmit.countDown();
            executor.shutdownNow();
        }
    }

    private void submitEvaluation(Long applicationId) {
        evaluationCommandService.submit(SubmitRecruitingApplicationEvaluationCommand.of(
            applicationId,
            EVALUATOR_MEMBER_ID,
            RecruitingEvaluatorStage.INTERVIEW,
            RecruitingApplicationEvaluationDecision.APPROVED,
            "제출"
        ));
    }

    private void saveEvaluationDraft(Long applicationId) {
        evaluationCommandService.saveDraft(SaveRecruitingApplicationEvaluationCommand.of(
            applicationId,
            EVALUATOR_MEMBER_ID,
            RecruitingEvaluatorStage.INTERVIEW,
            RecruitingApplicationEvaluationDecision.REJECTED,
            "늦은 초안"
        ));
    }

    private void updateRoundQuestion(Fixture fixture) {
        roundQuestionCommandService.updateRoundQuestion(UpdateRecruitingRoundInterviewQuestionCommand.of(
            fixture.roundQuestionId(),
            fixture.roundId(),
            MANAGER_MEMBER_ID,
            "늦은 공통 질문",
            1
        ));
    }

    private void updateApplicationQuestion(Fixture fixture) {
        applicationQuestionCommandService.updateApplicationQuestion(
            UpdateRecruitingApplicationInterviewQuestionCommand.of(
                fixture.applicationQuestionId(),
                fixture.applicationId(),
                EVALUATOR_MEMBER_ID,
                "늦은 개별 질문",
                1
            )
        );
    }

    private Fixture persistFixture() {
        long seed = FIXTURE_SEQUENCE.incrementAndGet();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> {
            RecruitingSeason season = seasonAdapter.save(RecruitingSeason.create(seed, seed));
            RecruitingRound round = roundAdapter.save(RecruitingRound.createRegular(season, configuration()));
            RecruitingApplicationForm form = formAdapter.save(RecruitingApplicationForm.create(round, seed));
            RecruitingApplication application = RecruitingApplication.createMemberDraft(
                form,
                seed,
                seed,
                RecruitingApplicantProfile.create(
                    round,
                    "지원자" + seed,
                    RecruitingApplicantEmail.from("applicant" + seed + "@example.com"),
                    ChallengerTrack.WEB_PRODUCT_ENGINEER,
                    null
                ),
                String.format("%06d", seed)
            );
            application.submit(seed);
            application.passDocument(MANAGER_MEMBER_ID, "서류 합격");
            application.assignInterview(MANAGER_MEMBER_ID, "면접 배정");
            applicationAdapter.save(application);
            RecruitingRoundInterviewQuestion roundQuestion = roundQuestionAdapter.save(
                RecruitingRoundInterviewQuestion.create(round, "기존 공통 질문", 0, MANAGER_MEMBER_ID)
            );
            RecruitingApplicationInterviewQuestion applicationQuestion = applicationQuestionAdapter.save(
                RecruitingApplicationInterviewQuestion.create(application, "기존 개별 질문", 0)
            );
            return new Fixture(
                round.getId(),
                application.getId(),
                roundQuestion.getId(),
                applicationQuestion.getId()
            );
        });
    }

    private RecruitingRoundConfiguration configuration() {
        return RecruitingRoundConfiguration.of(
            List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER),
            false,
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-08-08T00:00:00Z"),
            Instant.parse("2026-08-10T00:00:00Z"),
            true,
            Instant.parse("2026-08-11T00:00:00Z"),
            Instant.parse("2026-08-12T00:00:00Z"),
            Instant.parse("2026-08-16T00:00:00Z"),
            null,
            null,
            null
        );
    }

    private RecruitingApplicationEvaluationStatus evaluationStatus(Long applicationId) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> evaluationAdapter
            .findByApplicationIdAndEvaluatorMemberIdAndStage(
                applicationId,
                EVALUATOR_MEMBER_ID,
                RecruitingEvaluatorStage.INTERVIEW
            )
            .map(RecruitingApplicationEvaluation::getStatus)
            .orElseThrow());
    }

    private String roundQuestionContent(Long questionId) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> roundQuestionAdapter.getById(questionId).getContent());
    }

    private String applicationQuestionContent(Long questionId) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> applicationQuestionAdapter.getById(questionId).getContent());
    }

    @FunctionalInterface
    private interface CompetingAction {
        void run();
    }

    private record Fixture(
        Long roundId,
        Long applicationId,
        Long roundQuestionId,
        Long applicationQuestionId
    ) {
    }

    private record RacingResult(boolean submitSucceeded, RecruitingErrorCode competingError) {
    }
}
