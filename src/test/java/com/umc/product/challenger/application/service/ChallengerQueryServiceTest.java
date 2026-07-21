package com.umc.product.challenger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.challenger.application.port.in.query.GetChallengerPointUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.challenger.application.port.out.LoadChallengerPort;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;

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

    @Test
    @DisplayName("회원별 최신 챌린저를 상태가 포함된 경량 Info로 변환한다")
    void 회원별_최신_챌린저를_상태가_포함된_경량_Info로_변환한다() {
        // given
        Challenger active = Challenger.builder()
            .memberId(10L)
            .gisuId(5L)
            .part(ChallengerPart.WEB)
            .build();
        Challenger graduated = Challenger.builder()
            .memberId(20L)
            .gisuId(3L)
            .part(ChallengerPart.SPRINGBOOT)
            .build();
        graduated.changeStatus(ChallengerStatus.GRADUATED, 1L, "수료");
        given(loadChallengerPort.findLatestPerMember()).willReturn(List.of(active, graduated));

        // when
        List<ChallengerBasicInfo> result = sut.listLatestBasicPerMember();

        // then
        assertThat(result)
            .extracting(
                ChallengerBasicInfo::memberId,
                ChallengerBasicInfo::gisuId,
                ChallengerBasicInfo::part,
                ChallengerBasicInfo::challengerStatus
            )
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(
                    10L, 5L, ChallengerPart.WEB, ChallengerStatus.ACTIVE
                ),
                org.assertj.core.groups.Tuple.tuple(
                    20L, 3L, ChallengerPart.SPRINGBOOT, ChallengerStatus.GRADUATED
                )
            );
        then(loadChallengerPort).should().findLatestPerMember();
        then(getChallengerPointUseCase).shouldHaveNoInteractions();
    }
}
