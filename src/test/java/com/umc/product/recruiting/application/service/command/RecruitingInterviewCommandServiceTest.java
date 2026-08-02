package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.FindRecruitingInterviewScheduleCandidatesCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SkipRecruitingInterviewCommand;
import com.umc.product.recruiting.application.port.out.FindRecruitingScheduleOverlapPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingInterviewScheduleCandidate;
import com.umc.product.recruiting.application.port.out.dto.RecruitingScheduleOverlapSlot;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;

@ExtendWith(MockitoExtension.class)
class RecruitingInterviewCommandServiceTest {

    @Mock
    SaveRecruitingApplicationPort saveApplicationPort;

    @Mock
    LoadRecruitingInterviewSchedulePort loadSchedulePort;

    @Mock
    SaveRecruitingInterviewSchedulePort saveSchedulePort;

    @Mock
    FindRecruitingScheduleOverlapPort findScheduleOverlapPort;

    @Mock
    AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;

    @Mock
    RecruitingConcurrencyLockService concurrencyLockService;

    RecruitingInterviewCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new RecruitingInterviewCommandService(
            saveApplicationPort,
            loadSchedulePort,
            saveSchedulePort,
            findScheduleOverlapPort,
            authorizeManagementUseCase,
            concurrencyLockService
        );
    }

    @Test
    @DisplayName("면접 일정 후보 조회는 기존 overlap 경계에 위임한다")
    void 면접_일정_후보_조회는_기존_overlap_경계에_위임한다() {
        List<RecruitingInterviewScheduleCandidate> expected = List.of(
            new RecruitingInterviewScheduleCandidate(
                Instant.parse("2026-08-12T01:00:00Z"),
                Instant.parse("2026-08-12T01:15:00Z"),
                3
            )
        );
        given(findScheduleOverlapPort.findOverlaps(100L, 200L, List.of(1L, 2L))).willReturn(List.of(
            new RecruitingScheduleOverlapSlot(
                Instant.parse("2026-08-12T01:00:00Z"),
                java.util.Set.of(1L, 2L, 3L)
            )
        ));

        List<RecruitingInterviewScheduleCandidate> result = sut.findScheduleCandidates(
            FindRecruitingInterviewScheduleCandidatesCommand.builder()
                .formId(100L)
                .questionId(200L)
                .formResponseIds(List.of(1L, 2L))
                .build()
        );

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("면접 생략은 지원서 도메인 전이를 저장한다")
    void 면접_생략은_지원서_도메인_전이를_저장한다() {
        RecruitingApplication application = org.mockito.Mockito.mock(RecruitingApplication.class);
        com.umc.product.recruiting.domain.RecruitingRound round =
            org.mockito.Mockito.mock(com.umc.product.recruiting.domain.RecruitingRound.class);
        com.umc.product.recruiting.domain.RecruitingSeason season =
            org.mockito.Mockito.mock(com.umc.product.recruiting.domain.RecruitingSeason.class);
        given(concurrencyLockService.lockApplication(900L)).willReturn(application);
        given(application.getRound()).willReturn(round);
        given(round.getSeason()).willReturn(season);
        given(season.getId()).willReturn(700L);

        sut.skip(SkipRecruitingInterviewCommand.builder()
            .applicationId(900L)
            .skippedByMemberId(20L)
            .reason("면접 없음")
            .build());

        verify(application).skipInterview(20L, "면접 없음");
        verify(saveApplicationPort).save(application);
        verify(authorizeManagementUseCase).authorizeSeasonManagement(20L, 700L);
    }

    @Test
    @DisplayName("면접 생략은 이미 생성된 일정 요청을 CANCELLED로 보존한다")
    void 면접_생략은_기존_일정을_취소한다() {
        RecruitingApplication application = org.mockito.Mockito.mock(RecruitingApplication.class);
        com.umc.product.recruiting.domain.RecruitingRound round =
            org.mockito.Mockito.mock(com.umc.product.recruiting.domain.RecruitingRound.class);
        com.umc.product.recruiting.domain.RecruitingSeason season =
            org.mockito.Mockito.mock(com.umc.product.recruiting.domain.RecruitingSeason.class);
        RecruitingInterviewSchedule schedule = org.mockito.Mockito.mock(RecruitingInterviewSchedule.class);
        given(concurrencyLockService.lockApplication(900L)).willReturn(application);
        given(application.getId()).willReturn(900L);
        given(application.getRound()).willReturn(round);
        given(round.getSeason()).willReturn(season);
        given(season.getId()).willReturn(700L);
        given(loadSchedulePort.findByApplicationId(900L)).willReturn(Optional.of(schedule));

        sut.skip(SkipRecruitingInterviewCommand.builder()
            .applicationId(900L)
            .skippedByMemberId(20L)
            .reason("면접 없음")
            .build());

        verify(schedule).cancel();
        verify(saveSchedulePort).saveSchedule(schedule);
    }
}
