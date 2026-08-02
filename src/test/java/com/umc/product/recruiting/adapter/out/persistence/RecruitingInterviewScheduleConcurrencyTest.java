package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Set;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.member.adapter.out.persistence.MemberPersistenceAdapter;
import com.umc.product.member.adapter.out.persistence.MemberQueryRepository;
import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewSchedulesCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewSchedulesCommand.Assignment;
import com.umc.product.recruiting.application.port.out.FindRecruitingScheduleOverlapPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingScheduleOverlapSlot;
import com.umc.product.recruiting.application.service.command.RecruitingConcurrencyLockService;
import com.umc.product.recruiting.application.service.command.RecruitingInterviewScheduleConfirmationService;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.RecruitingInterviewSession;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;
import com.umc.product.support.PersistenceAdapterTest;
import com.umc.product.support.fixture.MemberFixture;

@PersistenceAdapterTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
    RecruitingSeasonPersistenceAdapter.class,
    RecruitingRoundPersistenceAdapter.class,
    RecruitingApplicationFormPersistenceAdapter.class,
    RecruitingApplicationPersistenceAdapter.class,
    RecruitingApplicationQueryRepository.class,
    RecruitingApplicantLockPersistenceAdapter.class,
    RecruitingInterviewSessionPersistenceAdapter.class,
    RecruitingInterviewSchedulePersistenceAdapter.class,
    MemberPersistenceAdapter.class,
    MemberQueryRepository.class,
    MemberFixture.class,
    RecruitingConcurrencyLockService.class,
    RecruitingInterviewScheduleConfirmationService.class
})
class RecruitingInterviewScheduleConcurrencyTest {

    private static final Instant SESSION_START = Instant.parse("2026-08-12T01:00:00Z");
    private static final AtomicLong SEQUENCE = new AtomicLong(8000L);

    @Autowired RecruitingSeasonPersistenceAdapter seasonAdapter;
    @Autowired RecruitingRoundPersistenceAdapter roundAdapter;
    @Autowired RecruitingApplicationFormPersistenceAdapter formAdapter;
    @Autowired RecruitingApplicationPersistenceAdapter applicationAdapter;
    @Autowired RecruitingInterviewSessionPersistenceAdapter sessionAdapter;
    @Autowired RecruitingInterviewSchedulePersistenceAdapter scheduleAdapter;
    @Autowired RecruitingInterviewScheduleConfirmationService confirmationService;
    @Autowired MemberFixture memberFixture;
    @Autowired PlatformTransactionManager transactionManager;

    @MockitoBean FindRecruitingScheduleOverlapPort findOverlapPort;
    @MockitoBean AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;

    @Test
    @DisplayName("동일 세션 슬롯을 동시에 확정하면 정확히 한 건만 성공한다")
    void sameSlotAllowsExactlyOneConfirmation() throws Exception {
        Fixture fixture = persistFixture(2, 1);
        allowAllSubmittedResponses();

        List<Result> results = race(
            command(fixture, 0, 0, SESSION_START),
            command(fixture, 1, 0, SESSION_START)
        );

        assertThat(results.stream().filter(Result::success)).hasSize(1);
        assertThat(results.stream().filter(result -> result.errorCode()
            == RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_ASSIGNMENT_CONFLICT)).hasSize(1);
        assertThat(confirmedCount(fixture.applicationIds())).isEqualTo(1);
    }

    @Test
    @DisplayName("서로 다른 세션 슬롯의 병렬 확정은 모두 성공한다")
    void differentSlotsBothSucceed() throws Exception {
        Fixture fixture = persistFixture(2, 1);
        allowAllSubmittedResponses();

        List<Result> results = race(
            command(fixture, 0, 0, SESSION_START),
            command(fixture, 1, 0, SESSION_START.plusSeconds(900))
        );

        assertThat(results).allMatch(Result::success);
        assertThat(confirmedCount(fixture.applicationIds())).isEqualTo(2);
    }

    @Test
    @DisplayName("동일 지원자를 서로 다른 세션에서 동시에 확정해도 한 건만 반영한다")
    void sameApplicationCannotBeConfirmedTwice() throws Exception {
        Fixture fixture = persistFixture(1, 2);
        allowAllSubmittedResponses();

        List<Result> results = race(
            command(fixture, 0, 0, SESSION_START),
            command(fixture, 0, 1, SESSION_START)
        );

        assertThat(results.stream().filter(Result::success)).hasSize(1);
        assertThat(results.stream().filter(result -> result.errorCode()
            == RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_ASSIGNMENT_CONFLICT)).hasSize(1);
        assertThat(confirmedCount(fixture.applicationIds())).isEqualTo(1);
    }

    @Test
    @DisplayName("batch 한 항목의 가능 시간이 틀리면 전체 변경을 rollback 한다")
    void invalidItemRollsBackWholeBatch() {
        Fixture fixture = persistFixture(2, 1);
        given(findOverlapPort.findOverlaps(
            org.mockito.ArgumentMatchers.eq(300L),
            org.mockito.ArgumentMatchers.eq(301L),
            anyList()
        )).willReturn(List.of(new RecruitingScheduleOverlapSlot(
            SESSION_START,
            Set.of(fixture.responseIds().getFirst())
        )));
        ConfirmRecruitingInterviewSchedulesCommand command = ConfirmRecruitingInterviewSchedulesCommand.of(
            fixture.roundId(),
            fixture.requesterMemberId(),
            List.of(
                assignment(fixture, 0, 0, SESSION_START),
                assignment(fixture, 1, 0, SESSION_START.plusSeconds(900))
            )
        );

        Result result = confirm(command, null, null);

        assertThat(result.errorCode()).isEqualTo(
            RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_ASSIGNMENT_CONFLICT
        );
        assertThat(confirmedCount(fixture.applicationIds())).isZero();
    }

    private void allowAllSubmittedResponses() {
        given(findOverlapPort.findOverlaps(
            org.mockito.ArgumentMatchers.eq(300L),
            org.mockito.ArgumentMatchers.eq(301L),
            anyList()
        )).willAnswer(invocation -> {
            List<Long> responseIds = invocation.getArgument(2);
            Set<Long> available = Set.copyOf(responseIds);
            return List.of(
                new RecruitingScheduleOverlapSlot(SESSION_START, available),
                new RecruitingScheduleOverlapSlot(SESSION_START.plusSeconds(900), available)
            );
        });
    }

    private List<Result> race(
        ConfirmRecruitingInterviewSchedulesCommand firstCommand,
        ConfirmRecruitingInterviewSchedulesCommand secondCommand
    ) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Result> first = executor.submit(() -> confirm(firstCommand, ready, start));
            Future<Result> second = executor.submit(() -> confirm(secondCommand, ready, start));
            ready.await();
            start.countDown();
            return List.of(first.get(), second.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private Result confirm(
        ConfirmRecruitingInterviewSchedulesCommand command,
        CountDownLatch ready,
        CountDownLatch start
    ) {
        try {
            if (ready != null) {
                ready.countDown();
                start.await();
            }
            confirmationService.confirmAll(command);
            return new Result(true, null);
        } catch (RecruitingDomainException exception) {
            return new Result(false, (RecruitingErrorCode) exception.getBaseCode());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private Fixture persistFixture(int applicationCount, int sessionCount) {
        long seed = SEQUENCE.incrementAndGet();
        return new TransactionTemplate(transactionManager).execute(status -> {
            RecruitingSeason season = seasonAdapter.save(RecruitingSeason.create(seed, seed));
            RecruitingRound round = roundAdapter.save(RecruitingRound.createRegular(season, configuration()));
            RecruitingApplicationForm form = formAdapter.save(RecruitingApplicationForm.create(round, seed));
            Long requesterMemberId = memberFixture.일반("요" + seed).getId();
            List<Long> applicationIds = java.util.stream.IntStream.range(0, applicationCount)
                .mapToObj(index -> persistApplication(form, round, requesterMemberId, seed, index))
                .toList();
            List<Long> responseIds = applicationIds.stream()
                .map(scheduleAdapter::getByApplicationId)
                .map(RecruitingInterviewSchedule::getAvailabilityFormResponseId)
                .toList();
            List<Long> sessionIds = java.util.stream.IntStream.range(0, sessionCount)
                .mapToObj(index -> sessionAdapter.save(RecruitingInterviewSession.create(
                    round.getId(),
                    "세션 " + index,
                    SESSION_START,
                    SESSION_START.plusSeconds(3600),
                    15,
                    RecruitingInterviewMode.OFFLINE,
                    "회의실 " + index,
                    round.getInterviewStartAt(),
                    round.getInterviewEndAt()
                )).getId())
                .toList();
            return new Fixture(round.getId(), requesterMemberId, applicationIds, responseIds, sessionIds);
        });
    }

    private Long persistApplication(
        RecruitingApplicationForm form,
        RecruitingRound round,
        Long requesterMemberId,
        long seed,
        int index
    ) {
        Long applicantMemberId = memberFixture.일반("지" + seed + index).getId();
        RecruitingApplication application = RecruitingApplication.createMemberDraft(
            form,
            seed * 10 + index + 1,
            applicantMemberId,
            RecruitingApplicantProfile.create(
                round,
                "지원자" + index,
                RecruitingApplicantEmail.from("batch" + seed + "-" + index + "@example.com"),
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                null
            ),
            String.format("%06d", seed + index)
        );
        application.submit(applicantMemberId);
        application.assignInterview(requesterMemberId, null);
        applicationAdapter.save(application);
        RecruitingInterviewSchedule schedule = RecruitingInterviewSchedule.requestAvailability(application, "contact");
        schedule.submitAvailability(seed * 10 + index + 1);
        scheduleAdapter.saveSchedule(schedule);
        return application.getId();
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
            Instant.parse("2026-08-15T00:00:00Z"),
            Instant.parse("2026-08-16T00:00:00Z"),
            300L,
            301L,
            null,
            "문의"
        );
    }

    private ConfirmRecruitingInterviewSchedulesCommand command(
        Fixture fixture,
        int applicationIndex,
        int sessionIndex,
        Instant startsAt
    ) {
        return ConfirmRecruitingInterviewSchedulesCommand.of(
            fixture.roundId(),
            fixture.requesterMemberId(),
            List.of(assignment(fixture, applicationIndex, sessionIndex, startsAt))
        );
    }

    private Assignment assignment(Fixture fixture, int applicationIndex, int sessionIndex, Instant startsAt) {
        return Assignment.of(
            fixture.applicationIds().get(applicationIndex),
            fixture.sessionIds().get(sessionIndex),
            startsAt,
            "confirmed contact"
        );
    }

    private long confirmedCount(List<Long> applicationIds) {
        return new TransactionTemplate(transactionManager).execute(status -> applicationIds.stream()
            .map(scheduleAdapter::getByApplicationId)
            .filter(schedule -> schedule.getStatus() == RecruitingInterviewScheduleStatus.CONFIRMED)
            .count());
    }

    private record Fixture(
        Long roundId,
        Long requesterMemberId,
        List<Long> applicationIds,
        List<Long> responseIds,
        List<Long> sessionIds
    ) {
    }

    private record Result(boolean success, RecruitingErrorCode errorCode) {
    }
}
