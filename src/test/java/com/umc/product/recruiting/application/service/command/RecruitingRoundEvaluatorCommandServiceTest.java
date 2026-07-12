package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingRoundEvaluatorCommandServiceTest {

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
    @DisplayName("학교 또는 중앙 모집 관리 권한을 확인한 뒤 평가자를 추가한다")
    void addEvaluatorAfterManagementAuthorization() {
        RecruitingSeason season = mock(RecruitingSeason.class);
        RecruitingRound round = mock(RecruitingRound.class);
        given(round.getSeason()).willReturn(season);
        given(season.getId()).willReturn(11L);
        given(loadRoundPort.getById(1L)).willReturn(round);
        given(saveEvaluatorPort.save(any())).willAnswer(invocation -> {
            RecruitingRoundEvaluator evaluator = invocation.getArgument(0);
            ReflectionTestUtils.setField(evaluator, "id", 100L);
            return evaluator;
        });

        Long evaluatorId = sut.addEvaluator(RecruitingRoundEvaluatorCommand.of(
            1L,
            99L,
            10L,
            RecruitingEvaluatorStage.DOCUMENT
        ));

        assertThat(evaluatorId).isEqualTo(100L);
        then(authorizeManagementUseCase).should().authorizeSeasonManagement(99L, 11L);
    }

    @Test
    @DisplayName("같은 차수 회원 단계의 평가자는 중복 추가할 수 없다")
    void rejectDuplicateEvaluator() {
        RecruitingSeason season = mock(RecruitingSeason.class);
        RecruitingRound round = mock(RecruitingRound.class);
        given(round.getSeason()).willReturn(season);
        given(season.getId()).willReturn(11L);
        given(loadRoundPort.getById(1L)).willReturn(round);
        given(loadEvaluatorPort.existsByRoundIdAndMemberIdAndStage(
            1L,
            10L,
            RecruitingEvaluatorStage.INTERVIEW
        )).willReturn(true);

        assertThatThrownBy(() -> sut.addEvaluator(RecruitingRoundEvaluatorCommand.of(
            1L,
            99L,
            10L,
            RecruitingEvaluatorStage.INTERVIEW
        )))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_EVALUATOR_ALREADY_EXISTS);
        then(saveEvaluatorPort).should(never()).save(any());
    }

    @Test
    @DisplayName("모집 관리 권한이 없으면 평가자 whitelist를 변경할 수 없다")
    void rejectEvaluatorMutationWithoutManagementPermission() {
        RecruitingSeason season = mock(RecruitingSeason.class);
        RecruitingRound round = mock(RecruitingRound.class);
        given(round.getSeason()).willReturn(season);
        given(season.getId()).willReturn(11L);
        given(loadRoundPort.getById(1L)).willReturn(round);
        willThrow(new AuthorizationDomainException(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED))
            .given(authorizeManagementUseCase).authorizeSeasonManagement(99L, 11L);

        assertThatThrownBy(() -> sut.addEvaluator(RecruitingRoundEvaluatorCommand.of(
            1L,
            99L,
            10L,
            RecruitingEvaluatorStage.INTERVIEW
        ))).isInstanceOf(AuthorizationDomainException.class);
        then(loadEvaluatorPort).shouldHaveNoInteractions();
        then(saveEvaluatorPort).shouldHaveNoInteractions();
    }
}
