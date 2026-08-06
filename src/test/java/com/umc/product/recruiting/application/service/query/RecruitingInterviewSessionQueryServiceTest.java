package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSessionPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.domain.RecruitingInterviewSession;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;

@ExtendWith(MockitoExtension.class)
class RecruitingInterviewSessionQueryServiceTest {

    private static final Instant START = Instant.parse("2026-08-10T00:00:00Z");

    @Mock LoadRecruitingInterviewSessionPort loadSessionPort;
    @Mock LoadRecruitingRoundPort loadRoundPort;
    @Mock AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;

    @Test
    @org.junit.jupiter.api.DisplayName("운영진 세션 목록은 영속성 포트의 시작 시각 순서를 유지한다")
    void listSessionsReturnsPortOrder() {
        RecruitingInterviewSessionQueryService sut = new RecruitingInterviewSessionQueryService(
            loadSessionPort,
            loadRoundPort,
            authorizeManagementUseCase
        );
        RecruitingRound round = round(1L, 11L);
        RecruitingInterviewSession first = session(START);
        RecruitingInterviewSession second = session(START.plusSeconds(1800));
        given(loadRoundPort.getById(1L)).willReturn(round);
        given(loadSessionPort.listByRoundId(1L)).willReturn(List.of(first, second));

        var result = sut.listSessions(1L, 99L);

        assertThat(result).extracting(info -> info.startsAt()).containsExactly(first.getStartsAt(), second.getStartsAt());
        then(authorizeManagementUseCase).should().authorizeSeasonManagement(99L, 11L);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("다른 모집 차수의 세션 단건 조회를 거부한다")
    void getSessionRejectsDifferentRound() {
        RecruitingInterviewSessionQueryService sut = new RecruitingInterviewSessionQueryService(
            loadSessionPort,
            loadRoundPort,
            authorizeManagementUseCase
        );
        RecruitingRound round = round(1L, 11L);
        given(loadRoundPort.getById(1L)).willReturn(round);
        given(loadSessionPort.getById(101L)).willReturn(RecruitingInterviewSession.create(
            2L, "세션", START, START.plusSeconds(900), 15, RecruitingInterviewMode.ONLINE, "링크",
            START, START.plusSeconds(3600)
        ));

        assertThatThrownBy(() -> sut.getSession(1L, 101L, 99L))
            .isInstanceOf(com.umc.product.recruiting.domain.exception.RecruitingDomainException.class);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("세션 목록 조회에는 모집 관리 권한이 필요하다")
    void listSessionsRequiresManagementPermission() {
        RecruitingInterviewSessionQueryService sut = new RecruitingInterviewSessionQueryService(
            loadSessionPort,
            loadRoundPort,
            authorizeManagementUseCase
        );
        RecruitingRound round = round(1L, 11L);
        given(loadRoundPort.getById(1L)).willReturn(round);
        org.mockito.BDDMockito.willThrow(
            new AuthorizationDomainException(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED)
        ).given(authorizeManagementUseCase).authorizeSeasonManagement(99L, 11L);

        assertThatThrownBy(() -> sut.listSessions(1L, 99L)).isInstanceOf(AuthorizationDomainException.class);
        then(loadSessionPort).shouldHaveNoInteractions();
    }

    private RecruitingRound round(Long id, Long seasonId) {
        RecruitingSeason season = mock(RecruitingSeason.class);
        RecruitingRound round = mock(RecruitingRound.class);
        org.mockito.Mockito.lenient().when(round.getId()).thenReturn(id);
        given(round.getSeason()).willReturn(season);
        given(season.getId()).willReturn(seasonId);
        return round;
    }

    private RecruitingInterviewSession session(Instant startsAt) {
        return RecruitingInterviewSession.create(
            1L, "세션", startsAt, startsAt.plusSeconds(900), 15, RecruitingInterviewMode.ONLINE, "링크",
            START, START.plusSeconds(7200)
        );
    }
}
