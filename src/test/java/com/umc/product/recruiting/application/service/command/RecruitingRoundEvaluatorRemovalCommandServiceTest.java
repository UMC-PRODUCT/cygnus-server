package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundEvaluatorCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundEvaluatorPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundEvaluatorPort;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundEvaluator;
import com.umc.product.recruiting.domain.RecruitingSeason;

@ExtendWith(MockitoExtension.class)
class RecruitingRoundEvaluatorRemovalCommandServiceTest {

    @Mock
    LoadRecruitingRoundPort loadRoundPort;

    @Mock
    LoadRecruitingRoundEvaluatorPort loadEvaluatorPort;

    @Mock
    SaveRecruitingRoundEvaluatorPort saveEvaluatorPort;

    @Mock
    AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;

    RecruitingRoundEvaluatorCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new RecruitingRoundEvaluatorCommandService(
            loadRoundPort,
            loadEvaluatorPort,
            saveEvaluatorPort,
            authorizeManagementUseCase
        );
    }

    @Test
    @DisplayName("실제 시즌 관리 권한을 확인한 뒤 평가자를 제거한다")
    void removeEvaluatorAfterActualSeasonAuthorization() {
        RecruitingRound round = roundInSeason(1L, 11L);
        RecruitingRoundEvaluator evaluator = RecruitingRoundEvaluator.create(round, 10L);
        given(loadEvaluatorPort.getByRoundIdAndMemberId(1L, 10L)).willReturn(evaluator);

        sut.removeEvaluator(RecruitingRoundEvaluatorCommand.of(
            1L,
            99L,
            10L
        ));

        then(authorizeManagementUseCase).should().authorizeSeasonManagement(99L, 11L);
        then(loadEvaluatorPort).should().getByRoundIdAndMemberId(1L, 10L);
        then(saveEvaluatorPort).should().delete(evaluator);
    }

    @Test
    @DisplayName("실제 시즌 관리 권한이 없으면 평가자 조회와 삭제 전에 중단한다")
    void rejectRemovalBeforeEvaluatorLookupWithoutActualSeasonPermission() {
        roundInSeason(1L, 11L);
        willThrow(new AuthorizationDomainException(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED))
            .given(authorizeManagementUseCase).authorizeSeasonManagement(99L, 11L);

        assertThatThrownBy(() -> sut.removeEvaluator(RecruitingRoundEvaluatorCommand.of(
            1L,
            99L,
            10L
        ))).isInstanceOf(AuthorizationDomainException.class);

        then(loadEvaluatorPort).shouldHaveNoInteractions();
        then(saveEvaluatorPort).shouldHaveNoInteractions();
    }

    private RecruitingRound roundInSeason(Long roundId, Long seasonId) {
        RecruitingSeason season = mock(RecruitingSeason.class);
        RecruitingRound round = mock(RecruitingRound.class);
        given(round.getSeason()).willReturn(season);
        given(season.getId()).willReturn(seasonId);
        given(loadRoundPort.getById(roundId)).willReturn(round);
        return round;
    }
}
