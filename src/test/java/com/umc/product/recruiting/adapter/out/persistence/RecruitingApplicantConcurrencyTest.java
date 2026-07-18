package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingFinalCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingDecisionStatus;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQuestionScopeUseCase;
import com.umc.product.recruiting.application.service.command.RecruitingApplicationCommandService;
import com.umc.product.recruiting.application.service.command.RecruitingApplicationKeyIssuer;
import com.umc.product.recruiting.application.service.command.RecruitingApplicationValidationService;
import com.umc.product.recruiting.application.service.command.RecruitingConcurrencyLockService;
import com.umc.product.recruiting.application.service.command.RecruitingDecisionCommandService;
import com.umc.product.recruiting.application.service.command.RecruitingInterviewAvailabilityRequestCoordinator;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;
import com.umc.product.support.PersistenceAdapterTest;
import com.umc.product.term.application.port.in.query.GetTermUseCase;

@PersistenceAdapterTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
    RecruitingSeasonPersistenceAdapter.class,
    RecruitingRoundPersistenceAdapter.class,
    RecruitingApplicationFormPersistenceAdapter.class,
    RecruitingApplicationPersistenceAdapter.class,
    RecruitingApplicationQueryRepository.class,
    RecruitingApplicantLockPersistenceAdapter.class,
    RecruitingConcurrencyLockService.class,
    RecruitingApplicationValidationService.class,
    RecruitingApplicationCommandService.class,
    RecruitingDecisionCommandService.class
})
class RecruitingApplicantConcurrencyTest {

    private static final Long CREATION_GISU_ID = 91L;
    private static final Long DECISION_GISU_ID = 92L;
    private static final Long APPLICANT_MEMBER_ID = 201L;
    private static final Long DECIDER_MEMBER_ID = 999L;
    private static final String APPLICANT_EMAIL = "same-applicant@example.com";
    private static final Instant TEST_NOW = Instant.parse("2026-07-15T00:00:00Z");

    @Autowired
    RecruitingSeasonPersistenceAdapter seasonAdapter;
    @Autowired
    RecruitingRoundPersistenceAdapter roundAdapter;
    @Autowired
    RecruitingApplicationFormPersistenceAdapter formAdapter;
    @Autowired
    RecruitingApplicationPersistenceAdapter applicationAdapter;
    @Autowired
    RecruitingApplicationCommandService applicationCommandService;
    @Autowired
    RecruitingDecisionCommandService decisionCommandService;
    @Autowired
    PlatformTransactionManager transactionManager;

    @MockitoBean
    ManageFormResponseUseCase manageFormResponseUseCase;
    @MockitoBean
    GetFormResponseUseCase getFormResponseUseCase;
    @MockitoBean
    GetRecruitingApplicationQuestionScopeUseCase getQuestionScopeUseCase;
    @MockitoBean
    RecruitingApplicationKeyIssuer applicationKeyIssuer;
    @MockitoBean
    GetChallengerRoleUseCase getChallengerRoleUseCase;
    @MockitoBean
    RecruitingInterviewAvailabilityRequestCoordinator availabilityRequestCoordinator;
    @MockitoBean
    GetTermUseCase getTermUseCase;
    @MockitoBean
    Clock clock;

    @Test
    @DisplayName("같은 기수 지원자가 다른 학교에 동시에 지원해도 하나만 생성된다")
    void onlyOneConcurrentApplicationAcrossSchoolsSucceeds() throws Exception {
        CreationFixture fixture = persistCreationFixture();
        given(clock.instant()).willReturn(TEST_NOW);
        given(manageFormResponseUseCase.createDraft(any())).willReturn(701L);
        given(applicationKeyIssuer.issue(APPLICANT_EMAIL)).willReturn("A1B2C3");

        List<Boolean> outcomes = race(
            () -> createDraft(fixture.firstFormId()),
            () -> createDraft(fixture.secondFormId())
        );

        assertThat(outcomes).containsExactlyInAnyOrder(true, false);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Long applicationCount = transaction.execute(status -> applicationAdapter.searchSummaryRows(
            CREATION_GISU_ID,
            null,
            null
        ).stream().filter(row -> APPLICANT_EMAIL.equals(row.applicantEmail())).count());
        assertThat(applicationCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("같은 기수 지원자를 다른 학교에서 동시에 최종 합격시켜도 하나만 성공한다")
    void onlyOneConcurrentFinalPassAcrossSchoolsSucceeds() throws Exception {
        DecisionFixture fixture = persistDecisionFixture();
        given(getChallengerRoleUseCase.isCentralCoreInGisu(DECIDER_MEMBER_ID, DECISION_GISU_ID))
            .willReturn(true);

        List<Boolean> outcomes = race(
            () -> decideFinal(fixture.firstApplicationId()),
            () -> decideFinal(fixture.secondApplicationId())
        );

        assertThat(outcomes).containsExactlyInAnyOrder(true, false);
        assertThat(applicationStatuses(fixture)).containsExactlyInAnyOrder(
            RecruitingApplicationStatus.FINAL_PASSED,
            RecruitingApplicationStatus.DOCUMENT_PASSED
        );
    }

    private boolean createDraft(Long formId) {
        try {
            applicationCommandService.createDraft(CreateRecruitingApplicationDraftCommand.builder()
                .applicationFormId(formId)
                .applicantMemberId(APPLICANT_MEMBER_ID)
                .applicantName("지원자")
                .applicantEmail(APPLICANT_EMAIL)
                .firstChoice(ChallengerTrack.WEB_PRODUCT_ENGINEER)
                .build());
            return true;
        } catch (RecruitingDomainException exception) {
            assertThat(exception.getBaseCode()).isIn(
                RecruitingErrorCode.RECRUITING_APPLICATION_DIFFERENT_SCHOOL_EXISTS,
                RecruitingErrorCode.RECRUITING_APPLICATION_REAPPLICATION_BLOCKED
            );
            return false;
        }
    }

    private boolean decideFinal(Long applicationId) {
        try {
            decisionCommandService.decideFinal(DecideRecruitingFinalCommand.builder()
                .applicationId(applicationId)
                .decision(RecruitingDecisionStatus.PASS)
                .acceptedTrack(ChallengerTrack.WEB_PRODUCT_ENGINEER)
                .decidedByMemberId(DECIDER_MEMBER_ID)
                .reason("최종 합격")
                .build());
            return true;
        } catch (RecruitingDomainException exception) {
            assertThat(exception.getBaseCode())
                .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_FINAL_PASS_ALREADY_EXISTS);
            return false;
        }
    }

    private List<Boolean> race(ThrowingBooleanSupplier first, ThrowingBooleanSupplier second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> firstFuture = executor.submit(() -> awaitAndRun(ready, start, first));
            Future<Boolean> secondFuture = executor.submit(() -> awaitAndRun(ready, start, second));
            ready.await();
            start.countDown();
            return List.of(firstFuture.get(), secondFuture.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean awaitAndRun(
        CountDownLatch ready,
        CountDownLatch start,
        ThrowingBooleanSupplier action
    ) throws Exception {
        ready.countDown();
        start.await();
        return action.getAsBoolean();
    }

    private CreationFixture persistCreationFixture() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> {
            RecruitingApplicationForm first = persistOpenForm(CREATION_GISU_ID, 101L);
            RecruitingApplicationForm second = persistOpenForm(CREATION_GISU_ID, 102L);
            return new CreationFixture(first.getId(), second.getId());
        });
    }

    private DecisionFixture persistDecisionFixture() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> {
            RecruitingApplication first = persistDocumentPassedApplication(101L, 801L, "G7H8I9");
            RecruitingApplication second = persistDocumentPassedApplication(102L, 802L, "J1K2L3");
            return new DecisionFixture(first.getId(), second.getId());
        });
    }

    private RecruitingApplicationForm persistOpenForm(Long gisuId, Long schoolId) {
        RecruitingSeason season = RecruitingSeason.create(gisuId, schoolId);
        season.activate();
        seasonAdapter.save(season);
        RecruitingRound round = RecruitingRound.createRegular(season, openConfiguration());
        round.open();
        roundAdapter.save(round);
        RecruitingApplicationForm form = RecruitingApplicationForm.create(round, 10_000L + schoolId);
        form.publish(form.getRound().getRecruitableTracks());
        return formAdapter.save(form);
    }

    private RecruitingApplication persistDocumentPassedApplication(
        Long schoolId,
        Long formResponseId,
        String applicationKey
    ) {
        RecruitingApplicationForm form = persistOpenForm(DECISION_GISU_ID, schoolId);
        RecruitingApplication application = RecruitingApplication.createMemberDraft(
            form,
            formResponseId,
            APPLICANT_MEMBER_ID,
            RecruitingApplicantProfile.create(
                form.getRound(),
                "지원자",
                RecruitingApplicantEmail.from(APPLICANT_EMAIL),
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                null
            ),
            applicationKey
        );
        application.submit(APPLICANT_MEMBER_ID);
        application.passDocument(DECIDER_MEMBER_ID, "서류 합격");
        return applicationAdapter.save(application);
    }

    private RecruitingRoundConfiguration openConfiguration() {
        return RecruitingRoundConfiguration.of(
            List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER),
            false,
            TEST_NOW.minus(1, ChronoUnit.DAYS),
            TEST_NOW.plus(1, ChronoUnit.DAYS),
            TEST_NOW.plus(2, ChronoUnit.DAYS),
            false,
            null,
            null,
            TEST_NOW.plus(3, ChronoUnit.DAYS),
            null,
            null,
            null
        );
    }

    private List<RecruitingApplicationStatus> applicationStatuses(DecisionFixture fixture) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> List.of(
            applicationAdapter.getById(fixture.firstApplicationId()).getStatus(),
            applicationAdapter.getById(fixture.secondApplicationId()).getStatus()
        ));
    }

    @FunctionalInterface
    private interface ThrowingBooleanSupplier {
        boolean getAsBoolean() throws Exception;
    }

    private record CreationFixture(Long firstFormId, Long secondFormId) {
    }

    private record DecisionFixture(Long firstApplicationId, Long secondApplicationId) {
    }
}
