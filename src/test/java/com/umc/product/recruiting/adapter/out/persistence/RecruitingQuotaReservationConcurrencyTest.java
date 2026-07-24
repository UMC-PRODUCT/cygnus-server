package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
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
import com.umc.product.challenger.application.port.in.command.AddChallengerTrackUseCase;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.authorization.RecruitingPolicyAction;
import com.umc.product.recruiting.application.authorization.RecruitingPolicyAuthorizationService;
import com.umc.product.recruiting.application.port.in.command.dto.PrepareRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.service.command.RecruitingRegistrationCommandService;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.RecruitingSeasonTrackQuota;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
    RecruitingSeasonPersistenceAdapter.class,
    RecruitingSeasonTrackQuotaPersistenceAdapter.class,
    RecruitingRoundPersistenceAdapter.class,
    RecruitingApplicationFormPersistenceAdapter.class,
    RecruitingApplicationPersistenceAdapter.class,
    RecruitingApplicationQueryRepository.class,
    RecruitingRegistrationCommandService.class
})
class RecruitingQuotaReservationConcurrencyTest {

    private static final Long EXECUTOR_MEMBER_ID = 999L;

    @Autowired
    RecruitingSeasonPersistenceAdapter seasonAdapter;
    @Autowired
    RecruitingSeasonTrackQuotaPersistenceAdapter quotaAdapter;
    @Autowired
    RecruitingRoundPersistenceAdapter roundAdapter;
    @Autowired
    RecruitingApplicationFormPersistenceAdapter formAdapter;
    @Autowired
    RecruitingApplicationPersistenceAdapter applicationAdapter;
    @Autowired
    RecruitingRegistrationCommandService registrationService;
    @Autowired
    PlatformTransactionManager transactionManager;

    @MockitoBean
    AddChallengerTrackUseCase addChallengerTrackUseCase;
    @MockitoBean
    GetChallengerRoleUseCase getChallengerRoleUseCase;
    @MockitoBean
    RecruitingPolicyAuthorizationService policyAuthorizationService;

    @Test
    @DisplayName("PostgreSQL에서 quota 1자리 병렬 READY 요청은 하나만 성공한다")
    void onlyOneConcurrentReadyWinsLastSeat() throws Exception {
        ReservationFixture fixture = persistFixture();
        given(policyAuthorizationService.evaluateMember(
            RecruitingPolicyAction.REGISTRATION_MANAGE,
            EXECUTOR_MEMBER_ID,
            fixture.gisuId(),
            null
        ))
            .willReturn(true);
        CountDownLatch workersReady = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> prepare(fixture.firstApplicationId(), workersReady, start));
            Future<Boolean> second = executor.submit(() -> prepare(fixture.secondApplicationId(), workersReady, start));
            workersReady.await();
            start.countDown();

            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Long usedCount = transaction.execute(status -> applicationAdapter
            .countReservedOrRegisteredBySeasonIdAndTrack(fixture.seasonId(), ChallengerTrack.DESIGN));
        assertThat(usedCount).isEqualTo(1L);
        assertThat(registrationStatuses(fixture)).containsExactlyInAnyOrder(
            RecruitingApplicationRegistrationStatus.READY,
            RecruitingApplicationRegistrationStatus.NOT_READY
        );
    }

    private boolean prepare(Long applicationId, CountDownLatch workersReady, CountDownLatch start) throws Exception {
        workersReady.countDown();
        start.await();
        try {
            registrationService.prepareRegistration(PrepareRecruitingRegistrationCommand.of(
                applicationId,
                EXECUTOR_MEMBER_ID
            ));
            return true;
        } catch (RecruitingDomainException exception) {
            if (exception.getBaseCode() == RecruitingErrorCode.RECRUITING_QUOTA_EXCEEDED) {
                return false;
            }
            throw exception;
        }
    }

    private List<RecruitingApplicationRegistrationStatus> registrationStatuses(ReservationFixture fixture) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> List.of(
            applicationAdapter.getById(fixture.firstApplicationId()).getRegistrationStatus(),
            applicationAdapter.getById(fixture.secondApplicationId()).getRegistrationStatus()
        ));
    }

    private ReservationFixture persistFixture() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> {
            RecruitingSeason season = seasonAdapter.save(RecruitingSeason.create(91L, 92L));
            quotaAdapter.saveAll(List.of(RecruitingSeasonTrackQuota.create(
                season,
                ChallengerTrack.DESIGN,
                1
            )));
            RecruitingRound round = roundAdapter.save(RecruitingRound.createRegular(season, configuration()));
            RecruitingApplicationForm form = formAdapter.save(RecruitingApplicationForm.create(round, 93L));
            RecruitingApplication first = applicationAdapter.save(finalPassedApplication(
                form,
                101L,
                201L,
                "first@example.com",
                "A1B2C3"
            ));
            RecruitingApplication second = applicationAdapter.save(finalPassedApplication(
                form,
                102L,
                202L,
                "second@example.com",
                "D4E5F6"
            ));
            return new ReservationFixture(season.getId(), season.getGisuId(), first.getId(), second.getId());
        });
    }

    private RecruitingApplication finalPassedApplication(
        RecruitingApplicationForm form,
        Long formResponseId,
        Long memberId,
        String email,
        String applicationKey
    ) {
        RecruitingApplication application = RecruitingApplication.createMemberDraft(
            form,
            formResponseId,
            memberId,
            RecruitingApplicantProfile.create(
                form.getRound(),
                "지원자" + memberId,
                RecruitingApplicantEmail.from(email),
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                ChallengerTrack.DESIGN
            ),
            applicationKey
        );
        application.submit(memberId);
        application.skipInterview(EXECUTOR_MEMBER_ID, "면접 미진행");
        application.passFinal(EXECUTOR_MEMBER_ID, "최종 합격", ChallengerTrack.DESIGN);
        return application;
    }

    private RecruitingRoundConfiguration configuration() {
        return RecruitingRoundConfiguration.of(
            List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER, ChallengerTrack.DESIGN),
            true,
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-08-08T00:00:00Z"),
            Instant.parse("2026-08-10T00:00:00Z"),
            false,
            null,
            null,
            Instant.parse("2026-08-16T00:00:00Z"),
            null,
            null,
            null
        );
    }

    private record ReservationFixture(
        Long seasonId,
        Long gisuId,
        Long firstApplicationId,
        Long secondApplicationId
    ) {
    }
}
