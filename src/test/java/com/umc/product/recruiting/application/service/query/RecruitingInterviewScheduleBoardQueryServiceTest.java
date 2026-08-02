package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.out.FindRecruitingScheduleOverlapPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewScheduleBoardPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSessionPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingInterviewScheduleBoardRow;
import com.umc.product.recruiting.application.port.out.dto.RecruitingScheduleOverlapSlot;
import com.umc.product.recruiting.domain.RecruitingInterviewSession;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;

@ExtendWith(MockitoExtension.class)
class RecruitingInterviewScheduleBoardQueryServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 10);
    private static final Instant KST_DAY_START = Instant.parse("2026-08-09T15:00:00Z");

    @Mock LoadRecruitingRoundPort loadRoundPort;
    @Mock LoadRecruitingInterviewSessionPort loadSessionPort;
    @Mock LoadRecruitingInterviewScheduleBoardPort loadBoardPort;
    @Mock FindRecruitingScheduleOverlapPort findOverlapPort;
    @Mock AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;

    @Test
    @DisplayName("KST 날짜에 시작하는 세션 슬롯별 대기 후보와 확정 지원자를 조합한다")
    void getBoardBuildsSlotsForKstDate() {
        RecruitingRound round = round(11L, 10L, 20L);
        RecruitingInterviewSession session = session(101L, KST_DAY_START, KST_DAY_START.plusSeconds(1800));
        List<RecruitingInterviewScheduleBoardRow> rows = List.of(
            new RecruitingInterviewScheduleBoardRow(1L, 1001L, "대기 지원자", 5001L,
                RecruitingInterviewScheduleStatus.AVAILABILITY_SUBMITTED, null, null),
            new RecruitingInterviewScheduleBoardRow(2L, 1002L, "확정 지원자", 5002L,
                RecruitingInterviewScheduleStatus.CONFIRMED, 101L, KST_DAY_START.plusSeconds(900))
        );
        given(loadRoundPort.getById(1L)).willReturn(round);
        given(loadSessionPort.listByRoundId(1L)).willReturn(List.of(session));
        given(loadBoardPort.listByRoundId(1L)).willReturn(rows);
        given(findOverlapPort.findOverlaps(10L, 20L, List.of(5001L)))
            .willReturn(List.of(new RecruitingScheduleOverlapSlot(KST_DAY_START, Set.of(5001L))));
        RecruitingInterviewScheduleBoardQueryService sut = sut();

        var result = sut.getBoard(1L, DATE, 99L);

        assertThat(result.pendingApplicants()).extracting(applicant -> applicant.applicationId())
            .containsExactly(1001L);
        assertThat(result.confirmedApplicants()).extracting(applicant -> applicant.applicationId())
            .containsExactly(1002L);
        assertThat(result.sessions()).singleElement().satisfies(info -> {
            assertThat(info.slots()).hasSize(2);
            assertThat(info.slots().get(0).availableApplicationIds()).containsExactly(1001L);
            assertThat(info.slots().get(0).assignedApplicant()).isNull();
            assertThat(info.slots().get(1).assignedApplicant().applicationId()).isEqualTo(1002L);
        });
        then(authorizeManagementUseCase).should().authorizeSeasonManagement(99L, 11L);
        then(findOverlapPort).should().findOverlaps(10L, 20L, List.of(5001L));
    }

    @Test
    @DisplayName("세션이 없는 레거시 확정 일정이 섞여도 현재 세션 배정과 대기 후보를 정상 반환한다")
    void getBoardIgnoresLegacyConfirmedScheduleWithoutSession() {
        RecruitingRound round = round(11L, 10L, 20L);
        RecruitingInterviewSession session = session(101L, KST_DAY_START, KST_DAY_START.plusSeconds(1800));
        List<RecruitingInterviewScheduleBoardRow> rows = List.of(
            new RecruitingInterviewScheduleBoardRow(1L, 1001L, "레거시 확정 지원자", null,
                RecruitingInterviewScheduleStatus.CONFIRMED, null, KST_DAY_START),
            new RecruitingInterviewScheduleBoardRow(2L, 1002L, "현재 세션 확정 지원자", 5002L,
                RecruitingInterviewScheduleStatus.CONFIRMED, 101L, KST_DAY_START.plusSeconds(900)),
            new RecruitingInterviewScheduleBoardRow(3L, 1003L, "대기 지원자", 5003L,
                RecruitingInterviewScheduleStatus.AVAILABILITY_SUBMITTED, null, null)
        );
        given(loadRoundPort.getById(1L)).willReturn(round);
        given(loadSessionPort.listByRoundId(1L)).willReturn(List.of(session));
        given(loadBoardPort.listByRoundId(1L)).willReturn(rows);
        given(findOverlapPort.findOverlaps(10L, 20L, List.of(5003L)))
            .willReturn(List.of(new RecruitingScheduleOverlapSlot(KST_DAY_START, Set.of(5003L))));

        var result = sut().getBoard(1L, DATE, 99L);

        assertThat(result.pendingApplicants()).extracting(applicant -> applicant.applicationId())
            .containsExactly(1003L);
        assertThat(result.confirmedApplicants()).extracting(applicant -> applicant.applicationId())
            .containsExactly(1002L);
        assertThat(result.sessions()).singleElement().satisfies(info -> {
            assertThat(info.slots()).hasSize(2);
            assertThat(info.slots().get(0).assignedApplicant()).isNull();
            assertThat(info.slots().get(1).assignedApplicant().applicationId()).isEqualTo(1002L);
            assertThat(info.slots().get(0).availableApplicationIds()).containsExactly(1003L);
        });
        then(findOverlapPort).should().findOverlaps(10L, 20L, List.of(5003L));
    }

    @Test
    @DisplayName("KST 날짜 경계 밖에서 시작하는 세션은 보드에서 제외한다")
    void getBoardFiltersSessionsByKstStartDate() {
        RecruitingRound round = round(11L, 10L, 20L);
        RecruitingInterviewSession before = session(100L, KST_DAY_START.minusSeconds(900), KST_DAY_START);
        RecruitingInterviewSession inside = session(101L, KST_DAY_START, KST_DAY_START.plusSeconds(900));
        RecruitingInterviewSession nextDay = session(102L, KST_DAY_START.plusSeconds(86400), KST_DAY_START.plusSeconds(87300));
        given(loadRoundPort.getById(1L)).willReturn(round);
        given(loadSessionPort.listByRoundId(1L)).willReturn(List.of(before, inside, nextDay));
        given(loadBoardPort.listByRoundId(1L)).willReturn(List.of());

        var result = sut().getBoard(1L, DATE, 99L);

        assertThat(result.sessions()).extracting(info -> info.sessionId()).containsExactly(101L);
        then(findOverlapPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("확정 일정이 세션 계산 슬롯과 일치하지 않으면 조회를 거부한다")
    void getBoardRejectsInvalidConfirmedSlot() {
        RecruitingRound round = round(11L, 10L, 20L);
        RecruitingInterviewSession session = session(101L, KST_DAY_START, KST_DAY_START.plusSeconds(1800));
        given(loadRoundPort.getById(1L)).willReturn(round);
        given(loadSessionPort.listByRoundId(1L)).willReturn(List.of(session));
        given(loadBoardPort.listByRoundId(1L)).willReturn(List.of(
            new RecruitingInterviewScheduleBoardRow(2L, 1002L, "확정 지원자", 5002L,
                RecruitingInterviewScheduleStatus.CONFIRMED, 999L, KST_DAY_START)
        ));

        assertThatThrownBy(() -> sut().getBoard(1L, DATE, 99L))
            .isInstanceOf(RecruitingDomainException.class);
    }

    private RecruitingInterviewScheduleBoardQueryService sut() {
        return new RecruitingInterviewScheduleBoardQueryService(
            loadRoundPort, loadSessionPort, loadBoardPort, findOverlapPort, authorizeManagementUseCase
        );
    }

    private RecruitingRound round(Long seasonId, Long formId, Long questionId) {
        RecruitingRound round = mock(RecruitingRound.class);
        RecruitingSeason season = mock(RecruitingSeason.class);
        given(round.getSeason()).willReturn(season);
        given(season.getId()).willReturn(seasonId);
        org.mockito.Mockito.lenient().when(round.getAvailabilityFormId()).thenReturn(formId);
        org.mockito.Mockito.lenient().when(round.getAvailabilityScheduleQuestionId()).thenReturn(questionId);
        return round;
    }

    private RecruitingInterviewSession session(Long id, Instant startsAt, Instant endsAt) {
        RecruitingInterviewSession session = RecruitingInterviewSession.create(
            1L, "세션", startsAt, endsAt, 15, RecruitingInterviewMode.ONLINE, "회의 링크",
            startsAt.minusSeconds(3600), endsAt.plusSeconds(3600)
        );
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }
}
