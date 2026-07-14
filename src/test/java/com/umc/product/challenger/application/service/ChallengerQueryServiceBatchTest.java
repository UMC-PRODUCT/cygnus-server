package com.umc.product.challenger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.challenger.application.port.in.query.GetChallengerPointUseCase;
import com.umc.product.challenger.application.port.out.LoadChallengerPort;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengerQueryService ID bulk 조회")
class ChallengerQueryServiceBatchTest {

    @Mock
    LoadChallengerPort loadChallengerPort;

    @Mock
    GetChallengerPointUseCase getChallengerPointUseCase;

    @InjectMocks
    ChallengerQueryService sut;

    @Test
    @DisplayName("여러 challenger ID는 포인트를 IN query 한 번으로 조립한다")
    void getAllByIdsLoadsPointsOnce() {
        Challenger first = challenger(1L, 11L);
        Challenger second = challenger(2L, 22L);
        given(loadChallengerPort.getAllByIds(Set.of(1L, 2L)))
            .willReturn(List.of(first, second));
        given(getChallengerPointUseCase.getMapByChallengerIds(Set.of(1L, 2L)))
            .willReturn(Map.of());

        assertThat(sut.batchGetByIds(Set.of(1L, 2L))).hasSize(2);

        then(getChallengerPointUseCase).should()
            .getMapByChallengerIds(Set.of(1L, 2L));
        then(getChallengerPointUseCase).should(never()).getListByChallengerId(1L);
        then(getChallengerPointUseCase).should(never()).getListByChallengerId(2L);
    }

    private static Challenger challenger(Long id, Long memberId) {
        Challenger challenger = Challenger.builder()
            .memberId(memberId)
            .gisuId(9L)
            .part(ChallengerPart.SPRINGBOOT)
            .build();
        ReflectionTestUtils.setField(challenger, "id", id);
        return challenger;
    }
}
