package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingInterviewSessionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeleteRecruitingInterviewSessionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingInterviewSessionCommand;
import com.umc.product.recruiting.application.port.out.CheckRecruitingInterviewSessionReferencePort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSessionPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewSessionPort;
import com.umc.product.recruiting.domain.RecruitingInterviewSession;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingInterviewSessionCommandServiceTest {

    private static final Instant ROUND_START = Instant.parse("2026-08-10T00:00:00Z");
    private static final Instant ROUND_END = Instant.parse("2026-08-11T00:00:00Z");

    @Mock LoadRecruitingInterviewSessionPort loadSessionPort;
    @Mock SaveRecruitingInterviewSessionPort saveSessionPort;
    @Mock CheckRecruitingInterviewSessionReferencePort checkReferencePort;
    @Mock AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;
    @Mock RecruitingConcurrencyLockService concurrencyLockService;

    RecruitingInterviewSessionCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new RecruitingInterviewSessionCommandService(
            loadSessionPort,
            saveSessionPort,
            checkReferencePort,
            authorizeManagementUseCase,
            concurrencyLockService
        );
    }

    @Test
    @DisplayName("운영진은 모집 차수 면접 기간 안에 15분 세션을 생성한다")
    void createSessionWithFixedSlotDuration() {
        RecruitingRound round = authorizedRound(1L, 11L);
        RecruitingInterviewSession saved = mock(RecruitingInterviewSession.class);
        given(saved.getId()).willReturn(101L);
        given(saveSessionPort.save(org.mockito.ArgumentMatchers.any())).willReturn(saved);

        Long id = sut.createSession(CreateRecruitingInterviewSessionCommand.of(
            1L,
            99L,
            "오전 면접",
            ROUND_START,
            ROUND_START.plusSeconds(3600),
            RecruitingInterviewMode.ONLINE,
            "https://meet.example.com/room"
        ));

        assertThat(id).isEqualTo(101L);
        ArgumentCaptor<RecruitingInterviewSession> captor = ArgumentCaptor.forClass(RecruitingInterviewSession.class);
        then(saveSessionPort).should().save(captor.capture());
        assertThat(captor.getValue().getRoundId()).isEqualTo(round.getId());
        assertThat(captor.getValue().getSlotDurationMinutes()).isEqualTo(15);
    }

    @Test
    @DisplayName("다른 모집 차수에 속한 세션은 수정할 수 없다")
    void rejectUpdateForDifferentRound() {
        authorizedRound(1L, 11L);
        given(loadSessionPort.getById(101L)).willReturn(session(2L));

        assertThatThrownBy(() -> sut.updateSession(updateCommand()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_SESSION_NOT_FOUND);
        then(saveSessionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("확정 일정이 없는 세션은 수정할 수 있다")
    void updateSessionWithoutConfirmedSchedule() {
        authorizedRound(1L, 11L);
        RecruitingInterviewSession session = session(1L);
        given(loadSessionPort.getById(101L)).willReturn(session);

        sut.updateSession(updateCommand());

        assertThat(session.getName()).isEqualTo("수정 세션");
        assertThat(session.getSlotDurationMinutes()).isEqualTo(15);
        then(saveSessionPort).should().save(session);
    }

    @Test
    @DisplayName("확정된 면접 일정이 연결된 세션은 수정할 수 없다")
    void rejectUpdateWhenConfirmedScheduleExists() {
        authorizedRound(1L, 11L);
        RecruitingInterviewSession session = session(1L);
        given(loadSessionPort.getById(101L)).willReturn(session);
        given(checkReferencePort.existsConfirmedBySessionId(101L)).willReturn(true);

        assertThatThrownBy(() -> sut.updateSession(updateCommand()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_SESSION_CONFIRMED_SCHEDULE_EXISTS);
        assertThat(session.getName()).isEqualTo("기존 세션");
        then(saveSessionPort).should(never()).save(session);
    }

    @Test
    @DisplayName("참조 중인 면접 일정이 있는 세션은 FK 안전을 위해 삭제할 수 없다")
    void rejectDeleteWhenScheduleReferenceExists() {
        authorizedRound(1L, 11L);
        RecruitingInterviewSession session = session(1L);
        given(loadSessionPort.getById(101L)).willReturn(session);
        given(checkReferencePort.existsBySessionId(101L)).willReturn(true);

        assertThatThrownBy(() -> sut.deleteSession(DeleteRecruitingInterviewSessionCommand.of(101L, 1L, 99L)))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_SESSION_CONFIRMED_SCHEDULE_EXISTS);
        then(saveSessionPort).should(never()).delete(session);
    }

    @Test
    @DisplayName("면접 일정이 참조하지 않는 세션은 hard delete 한다")
    void deleteSessionWithoutScheduleReference() {
        authorizedRound(1L, 11L);
        RecruitingInterviewSession session = session(1L);
        given(loadSessionPort.getById(101L)).willReturn(session);

        sut.deleteSession(DeleteRecruitingInterviewSessionCommand.of(101L, 1L, 99L));

        then(saveSessionPort).should().delete(session);
    }

    private RecruitingRound authorizedRound(Long roundId, Long seasonId) {
        RecruitingSeason season = mock(RecruitingSeason.class);
        RecruitingRound round = mock(RecruitingRound.class);
        org.mockito.Mockito.lenient().when(round.getId()).thenReturn(roundId);
        given(round.getSeason()).willReturn(season);
        org.mockito.Mockito.lenient().when(round.getInterviewStartAt()).thenReturn(ROUND_START);
        org.mockito.Mockito.lenient().when(round.getInterviewEndAt()).thenReturn(ROUND_END);
        given(season.getId()).willReturn(seasonId);
        given(concurrencyLockService.lockRound(roundId)).willReturn(round);
        return round;
    }

    private RecruitingInterviewSession session(Long roundId) {
        RecruitingInterviewSession session = RecruitingInterviewSession.create(
            roundId,
            "기존 세션",
            ROUND_START,
            ROUND_START.plusSeconds(3600),
            15,
            RecruitingInterviewMode.OFFLINE,
            "회의실",
            ROUND_START,
            ROUND_END
        );
        ReflectionTestUtils.setField(session, "id", 101L);
        return session;
    }

    private UpdateRecruitingInterviewSessionCommand updateCommand() {
        return UpdateRecruitingInterviewSessionCommand.of(
            101L,
            1L,
            99L,
            "수정 세션",
            ROUND_START.plusSeconds(900),
            ROUND_START.plusSeconds(4500),
            RecruitingInterviewMode.ONLINE,
            "https://meet.example.com/new"
        );
    }
}
