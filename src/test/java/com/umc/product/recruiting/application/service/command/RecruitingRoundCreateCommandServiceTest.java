package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundConfigurationCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonTrackQuotaPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundPort;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.RecruitingSeasonTrackQuota;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingRoundCreateCommandServiceTest {

    @Mock
    LoadRecruitingSeasonPort loadSeasonPort;
    @Mock
    LoadRecruitingRoundPort loadRoundPort;
    @Mock
    SaveRecruitingRoundPort saveRoundPort;
    @Mock
    LoadRecruitingSeasonTrackQuotaPort loadQuotaPort;
    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;
    @InjectMocks
    RecruitingRoundCommandService sut;

    @Test
    @DisplayName("양수 쿼터 트랙으로 추가 모집 차수를 생성한다")
    void createAdditionalRound() {
        RecruitingSeason season = season(10L);
        given(loadSeasonPort.getByIdForUpdate(10L)).willReturn(season);
        given(loadRoundPort.getMaxAdditionalRoundNo(10L)).willReturn(1);
        given(loadQuotaPort.listBySeasonId(10L)).willReturn(List.of(quota(season, ChallengerTrack.PLAN, 3)));
        given(saveRoundPort.save(any())).willAnswer(invocation -> {
            RecruitingRound round = invocation.getArgument(0);
            ReflectionTestUtils.setField(round, "id", 200L);
            return round;
        });

        Long id = sut.createRound(command(RecruitingRoundType.ADDITIONAL, 2, ChallengerTrack.PLAN));

        assertThat(id).isEqualTo(200L);
    }

    @Test
    @DisplayName("같은 시즌 타입 차수는 중복 생성할 수 없다")
    void createRoundRejectsDuplicate() {
        given(loadRoundPort.existsBySeasonIdAndTypeAndRoundNo(10L, RecruitingRoundType.REGULAR, 1))
            .willReturn(true);

        assertThatThrownBy(() -> sut.createRound(command(
            RecruitingRoundType.REGULAR,
            null,
            ChallengerTrack.PLAN
        )))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_ALREADY_EXISTS);
        then(saveRoundPort).should(never()).save(any());
    }

    @Test
    @DisplayName("시즌 쿼터에 없는 트랙으로 차수를 생성할 수 없다")
    void createRoundRejectsTrackOutsideSeasonQuota() {
        RecruitingSeason season = season(10L);
        given(loadSeasonPort.getByIdForUpdate(10L)).willReturn(season);
        given(loadQuotaPort.listBySeasonId(10L)).willReturn(List.of(quota(season, ChallengerTrack.PLAN, 3)));

        assertThatThrownBy(() -> sut.createRound(command(
            RecruitingRoundType.REGULAR,
            null,
            ChallengerTrack.DESIGN
        )))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_TRACK_NOT_IN_SEASON);
    }

    @Test
    @DisplayName("목표 인원이 0명인 트랙으로 차수를 생성할 수 없다")
    void createRoundRejectsZeroTargetQuotaTrack() {
        RecruitingSeason season = season(10L);
        given(loadSeasonPort.getByIdForUpdate(10L)).willReturn(season);
        given(loadQuotaPort.listBySeasonId(10L)).willReturn(List.of(quota(season, ChallengerTrack.PLAN, 0)));
        lenient().when(saveRoundPort.save(any())).thenAnswer(invocation -> {
            RecruitingRound round = invocation.getArgument(0);
            ReflectionTestUtils.setField(round, "id", 201L);
            return round;
        });

        assertThatThrownBy(() -> {
            Long savedRoundId = sut.createRound(command(
                RecruitingRoundType.REGULAR,
                null,
                ChallengerTrack.PLAN
            ));
            throw new AssertionError("목표 인원 0명 트랙의 invalid Round가 저장됨: id=" + savedRoundId);
        })
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_TRACK_NOT_IN_SEASON);
    }

    @Test
    @DisplayName("REST와 GraphQL이 공유하는 생성 UseCase는 INFRA_PLUS 모집을 거부한다")
    void createRoundRejectsInfraPlusBeforeQuotaValidation() {
        RecruitingSeason season = season(10L);
        given(loadSeasonPort.getByIdForUpdate(10L)).willReturn(season);

        assertThatThrownBy(() -> sut.createRound(command(
            RecruitingRoundType.REGULAR,
            null,
            ChallengerTrack.INFRA_PLUS
        )))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_INVALID_TRACKS);

        then(loadQuotaPort).shouldHaveNoInteractions();
        then(saveRoundPort).shouldHaveNoInteractions();
    }

    private CreateRecruitingRoundCommand command(
        RecruitingRoundType type,
        Integer roundNo,
        ChallengerTrack track
    ) {
        return CreateRecruitingRoundCommand.builder()
            .seasonId(10L)
            .type(type)
            .roundNo(roundNo)
            .title(type == RecruitingRoundType.REGULAR ? "본모집" : "추가모집 " + roundNo + "차")
            .configuration(configuration(track))
            .build();
    }

    private RecruitingRoundConfigurationCommand configuration(ChallengerTrack track) {
        return RecruitingRoundConfigurationCommand.of(
            List.of(track),
            false,
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-08-08T00:00:00Z"),
            Instant.parse("2026-08-10T00:00:00Z"),
            false,
            null,
            null,
            Instant.parse("2026-08-16T00:00:00Z"),
            null,
            null,
            null
        );
    }

    private RecruitingSeason season(Long id) {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        ReflectionTestUtils.setField(season, "id", id);
        return season;
    }

    private RecruitingSeasonTrackQuota quota(
        RecruitingSeason season,
        ChallengerTrack track,
        int targetCount
    ) {
        return RecruitingSeasonTrackQuota.create(season, track, targetCount);
    }
}
