package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewSchedulesCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewSchedulesCommand.Assignment;
import com.umc.product.recruiting.application.port.out.FindRecruitingScheduleOverlapPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSessionPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingScheduleOverlapSlot;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.RecruitingInterviewSession;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingInterviewScheduleBatchCommandServiceTest {

    private static final Instant START = Instant.parse("2026-08-12T01:00:00Z");

    @Mock LoadRecruitingRoundPort loadRoundPort;
    @Mock LoadRecruitingInterviewSessionPort loadSessionPort;
    @Mock LoadRecruitingInterviewSchedulePort loadSchedulePort;
    @Mock SaveRecruitingInterviewSchedulePort saveSchedulePort;
    @Mock FindRecruitingScheduleOverlapPort findOverlapPort;
    @Mock AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;
    @Mock RecruitingConcurrencyLockService concurrencyLockService;
    @Mock RecruitingRound round;
    @Mock RecruitingSeason season;
    @Mock RecruitingInterviewSession session;
    @Mock RecruitingApplication application;
    @Mock RecruitingInterviewSchedule schedule;

    RecruitingInterviewScheduleConfirmationService sut;

    @BeforeEach
    void setUp() {
        sut = new RecruitingInterviewScheduleConfirmationService(
            loadRoundPort,
            loadSessionPort,
            loadSchedulePort,
            saveSchedulePort,
            findOverlapPort,
            authorizeManagementUseCase,
            concurrencyLockService
        );
    }

    @Test
    @DisplayName("batch 확정은 session, application, schedule 순서로 잠그고 세션 값으로 확정한다")
    void batchConfirmUsesDeterministicLocksAndSessionValues() {
        givenValidLockedState();
        given(findOverlapPort.findOverlaps(300L, 301L, List.of(700L)))
            .willReturn(List.of(new RecruitingScheduleOverlapSlot(START, Set.of(700L))));

        sut.confirmAll(command(List.of(assignment(900L, 101L, START))));

        InOrder lockOrder = inOrder(loadSessionPort, concurrencyLockService, loadSchedulePort);
        lockOrder.verify(loadSessionPort).getAllByIdsForUpdate(List.of(101L));
        lockOrder.verify(concurrencyLockService).lockApplications(List.of(900L));
        lockOrder.verify(loadSchedulePort).getAllByApplicationIdsForUpdate(List.of(900L));
        verify(schedule).confirm(101L, START, START.plusSeconds(900), "회의실 A", "contact");
        verify(saveSchedulePort).saveAllAndFlush(List.of(schedule));
    }

    @Test
    @DisplayName("한 항목의 가능 시간이 불일치하면 어느 일정도 확정하지 않는다")
    void invalidAvailabilityLeavesWholeBatchUnchanged() {
        givenValidLockedState();
        given(findOverlapPort.findOverlaps(300L, 301L, List.of(700L))).willReturn(List.of());

        assertRecruitingError(
            () -> sut.confirmAll(command(List.of(assignment(900L, 101L, START)))),
            RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_ASSIGNMENT_CONFLICT
        );

        verify(schedule, never()).confirm(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        );
        verifyNoInteractions(saveSchedulePort);
    }

    @Test
    @DisplayName("동일 지원자나 동일 세션 슬롯이 중복된 batch는 잠금 전에 거부한다")
    void duplicateApplicationOrSlotIsRejectedBeforeLock() {
        Assignment first = assignment(900L, 101L, START);
        Assignment duplicateApplication = assignment(900L, 102L, START.plusSeconds(900));

        assertRecruitingError(
            () -> sut.confirmAll(command(List.of(first, duplicateApplication))),
            RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_ASSIGNMENT_CONFLICT
        );

        verifyNoInteractions(loadRoundPort, loadSessionPort, loadSchedulePort, concurrencyLockService);
    }

    @Test
    @DisplayName("100건을 초과한 batch는 잠금 전에 거부한다")
    void rejectBatchLargerThanMaximumBeforeLock() {
        List<Assignment> assignments = IntStream.range(0,
                ConfirmRecruitingInterviewSchedulesCommand.MAX_ASSIGNMENT_COUNT + 1)
            .mapToObj(index -> assignment(900L + index, 101L + index, START.plusSeconds(index * 900L)))
            .toList();

        assertRecruitingError(
            () -> sut.confirmAll(command(assignments)),
            RecruitingErrorCode.RECRUITING_INTERVIEW_SESSION_INVALID
        );

        verifyNoInteractions(loadRoundPort, loadSessionPort, loadSchedulePort, concurrencyLockService);
    }

    @Test
    @DisplayName("이미 확정된 세션 슬롯은 저장 전에 충돌로 거부한다")
    void occupiedSlotIsRejectedBeforeSave() {
        givenValidLockedState();
        given(findOverlapPort.findOverlaps(300L, 301L, List.of(700L)))
            .willReturn(List.of(new RecruitingScheduleOverlapSlot(START, Set.of(700L))));
        given(loadSchedulePort.existsConfirmedBySessionIdAndStartsAt(101L, START)).willReturn(true);

        assertRecruitingError(
            () -> sut.confirmAll(command(List.of(assignment(900L, 101L, START)))),
            RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_ASSIGNMENT_CONFLICT
        );

        verifyNoInteractions(saveSchedulePort);
    }

    private void givenValidLockedState() {
        given(loadRoundPort.getById(800L)).willReturn(round);
        given(round.getId()).willReturn(800L);
        given(round.getSeason()).willReturn(season);
        given(season.getId()).willReturn(700L);
        given(round.getAvailabilityFormId()).willReturn(300L);
        given(round.getAvailabilityScheduleQuestionId()).willReturn(301L);

        given(loadSessionPort.getAllByIdsForUpdate(List.of(101L))).willReturn(List.of(session));
        given(session.getId()).willReturn(101L);
        given(session.getRoundId()).willReturn(800L);
        given(session.getStartsAt()).willReturn(START);
        given(session.getEndsAt()).willReturn(START.plusSeconds(3600));
        given(session.getSlotDurationMinutes()).willReturn(15);
        org.mockito.Mockito.lenient().when(session.getLocation()).thenReturn("회의실 A");

        given(concurrencyLockService.lockApplications(List.of(900L))).willReturn(List.of(application));
        given(application.getId()).willReturn(900L);
        given(application.getRound()).willReturn(round);
        given(application.getStatus()).willReturn(RecruitingApplicationStatus.INTERVIEW_ASSIGNED);

        given(loadSchedulePort.getAllByApplicationIdsForUpdate(List.of(900L))).willReturn(List.of(schedule));
        given(schedule.getApplication()).willReturn(application);
        given(schedule.getStatus()).willReturn(RecruitingInterviewScheduleStatus.AVAILABILITY_SUBMITTED);
        given(schedule.getAvailabilityFormResponseId()).willReturn(700L);
    }

    private ConfirmRecruitingInterviewSchedulesCommand command(List<Assignment> assignments) {
        return ConfirmRecruitingInterviewSchedulesCommand.of(800L, 99L, assignments);
    }

    private Assignment assignment(Long applicationId, Long sessionId, Instant startsAt) {
        return Assignment.of(applicationId, sessionId, startsAt, "contact");
    }

    private void assertRecruitingError(Runnable action, RecruitingErrorCode expected) {
        assertThatThrownBy(action::run)
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(expected);
    }
}
