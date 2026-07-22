package com.umc.product.challenger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.challenger.application.port.in.query.GetChallengerPointUseCase;
import com.umc.product.challenger.application.port.out.LoadChallengerPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengerQueryService")
class ChallengerQueryServiceTest {

    @Mock
    LoadChallengerPort loadChallengerPort;

    @Mock
    GetChallengerPointUseCase getChallengerPointUseCase;

    @InjectMocks
    ChallengerQueryService sut;

    @Test
    @DisplayName("memberId 기준 챌린저 이력 존재 여부를 확인한다")
    void memberId_기준_챌린저_이력_존재_여부를_확인한다() {
        given(loadChallengerPort.existsByMemberId(1L)).willReturn(true);

        boolean result = sut.hasChallengerHistory(1L);

        assertThat(result).isTrue();
        then(loadChallengerPort).should().existsByMemberId(1L);
    }

    @Test
    @DisplayName("memberId가 없으면 챌린저 이력이 없다고 판단한다")
    void memberId가_없으면_챌린저_이력이_없다고_판단한다() {
        boolean result = sut.hasChallengerHistory(null);

        assertThat(result).isFalse();
        then(loadChallengerPort).shouldHaveNoInteractions();
    }

}
