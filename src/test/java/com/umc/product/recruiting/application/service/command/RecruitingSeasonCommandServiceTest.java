package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingSeasonCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundStatusCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingSeasonStatusCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingSeasonPort;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;
import com.umc.product.recruiting.domain.enums.RecruitingSeasonStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingSeasonCommandServiceTest {

    @Mock
    LoadRecruitingSeasonPort loadSeasonPort;

    @Mock
    SaveRecruitingSeasonPort saveSeasonPort;

    @Mock
    LoadRecruitingRoundPort loadRoundPort;

    @Mock
    SaveRecruitingRoundPort saveRoundPort;

    @InjectMocks
    RecruitingSeasonCommandService sut;

    @Test
    @DisplayName("학교와_기수_조합이_없으면_모집_시즌을_생성한다")
    void createSeasonWhenUnique() {
        // Given
        CreateRecruitingSeasonCommand command = CreateRecruitingSeasonCommand.builder()
            .gisuId(1L)
            .schoolId(10L)
            .build();
        given(loadSeasonPort.existsByGisuIdAndSchoolId(1L, 10L)).willReturn(false);
        given(saveSeasonPort.save(any())).willAnswer(invocation -> {
            RecruitingSeason season = invocation.getArgument(0);
            ReflectionTestUtils.setField(season, "id", 100L);
            return season;
        });

        // When
        Long seasonId = sut.createSeason(command);

        // Then
        assertThat(seasonId).isEqualTo(100L);
        ArgumentCaptor<RecruitingSeason> captor = ArgumentCaptor.forClass(RecruitingSeason.class);
        then(saveSeasonPort).should().save(captor.capture());
        assertThat(captor.getValue().getGisuId()).isEqualTo(1L);
        assertThat(captor.getValue().getSchoolId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("같은_학교와_기수의_모집_시즌이_이미_있으면_생성할_수_없다")
    void createSeasonRejectsDuplicate() {
        // Given
        CreateRecruitingSeasonCommand command = CreateRecruitingSeasonCommand.builder()
            .gisuId(1L)
            .schoolId(10L)
            .build();
        given(loadSeasonPort.existsByGisuIdAndSchoolId(1L, 10L)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> sut.createSeason(command))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_SEASON_ALREADY_EXISTS);
        then(saveSeasonPort).should(never()).save(any());
    }

    @Test
    @DisplayName("추가모집_차수가_중복되지_않으면_모집_차수를_생성한다")
    void createAdditionalRoundWhenUnique() {
        // Given
        RecruitingSeason season = season(10L);
        CreateRecruitingRoundCommand command = CreateRecruitingRoundCommand.builder()
            .seasonId(10L)
            .type(RecruitingRoundType.ADDITIONAL)
            .roundNo(2)
            .build();
        given(loadSeasonPort.getById(10L)).willReturn(season);
        given(loadRoundPort.existsBySeasonIdAndTypeAndRoundNo(10L, RecruitingRoundType.ADDITIONAL, 2))
            .willReturn(false);
        given(saveRoundPort.save(any())).willAnswer(invocation -> {
            RecruitingRound round = invocation.getArgument(0);
            ReflectionTestUtils.setField(round, "id", 200L);
            return round;
        });

        // When
        Long roundId = sut.createRound(command);

        // Then
        assertThat(roundId).isEqualTo(200L);
        ArgumentCaptor<RecruitingRound> captor = ArgumentCaptor.forClass(RecruitingRound.class);
        then(saveRoundPort).should().save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(RecruitingRoundType.ADDITIONAL);
        assertThat(captor.getValue().getRoundNo()).isEqualTo(2);
    }

    @Test
    @DisplayName("같은_시즌_타입_차수의_모집_차수가_이미_있으면_생성할_수_없다")
    void createRoundRejectsDuplicate() {
        // Given
        CreateRecruitingRoundCommand command = CreateRecruitingRoundCommand.builder()
            .seasonId(10L)
            .type(RecruitingRoundType.REGULAR)
            .build();
        given(loadRoundPort.existsBySeasonIdAndTypeAndRoundNo(10L, RecruitingRoundType.REGULAR, 1))
            .willReturn(true);

        // When & Then
        assertThatThrownBy(() -> sut.createRound(command))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_ALREADY_EXISTS);
        then(saveRoundPort).should(never()).save(any());
    }

    @Test
    @DisplayName("모집_시즌과_차수의_상태를_도메인_메소드로_변경한다")
    void updateSeasonAndRoundStatus() {
        // Given
        RecruitingSeason season = season(10L);
        RecruitingRound round = regularRound(20L, season);
        given(loadSeasonPort.getById(10L)).willReturn(season);
        given(loadRoundPort.getById(20L)).willReturn(round);

        // When
        sut.updateSeasonStatus(UpdateRecruitingSeasonStatusCommand.builder()
            .seasonId(10L)
            .status(RecruitingSeasonStatus.ACTIVE)
            .build());
        sut.updateRoundStatus(UpdateRecruitingRoundStatusCommand.builder()
            .roundId(20L)
            .status(RecruitingRoundStatus.OPEN)
            .build());

        // Then
        assertThat(season.getStatus()).isEqualTo(RecruitingSeasonStatus.ACTIVE);
        assertThat(round.getStatus()).isEqualTo(RecruitingRoundStatus.OPEN);
        then(saveSeasonPort).should().save(season);
        then(saveRoundPort).should().save(round);
    }

    private RecruitingSeason season(Long id) {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        ReflectionTestUtils.setField(season, "id", id);
        return season;
    }

    private RecruitingRound regularRound(Long id, RecruitingSeason season) {
        RecruitingRound round = RecruitingRound.createRegular(season);
        ReflectionTestUtils.setField(round, "id", id);
        return round;
    }
}
