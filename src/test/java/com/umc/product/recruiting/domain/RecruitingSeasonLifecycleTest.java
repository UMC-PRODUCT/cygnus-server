package com.umc.product.recruiting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingSeasonStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

class RecruitingSeasonLifecycleTest {

    @Test
    @DisplayName("새 모집 시즌은 DRAFT 상태이다")
    void createdSeasonIsDraft() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);

        assertThat(season.getStatus()).isEqualTo(RecruitingSeasonStatus.DRAFT);
    }

    @Test
    @DisplayName("모집 시즌은 DRAFT에서 ACTIVE로 전이한다")
    void seasonActivatesFromDraft() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);

        season.activate();

        assertThat(season.getStatus()).isEqualTo(RecruitingSeasonStatus.ACTIVE);
    }

    @Test
    @DisplayName("모집 시즌은 ACTIVE에서 CLOSED로 전이한다")
    void seasonClosesFromActive() {
        RecruitingSeason season = activeSeason();

        season.close();

        assertThat(season.getStatus()).isEqualTo(RecruitingSeasonStatus.CLOSED);
    }

    @Test
    @DisplayName("모집 시즌은 상태 전이 순서를 건너뛸 수 없다")
    void seasonRejectsInvalidTransition() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);

        assertThatThrownBy(season::close)
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_SEASON_INVALID_TRANSITION);
    }

    @Test
    @DisplayName("새 모집 차수는 DRAFT 상태이다")
    void createdRoundIsDraft() {
        RecruitingRound round = RecruitingRound.createRegular(RecruitingSeason.create(1L, 10L));

        assertThat(round.getStatus()).isEqualTo(RecruitingRoundStatus.DRAFT);
    }

    @Test
    @DisplayName("모집 차수는 DRAFT에서 OPEN으로 전이한다")
    void roundOpensFromDraft() {
        RecruitingRound round = RecruitingRound.createRegular(RecruitingSeason.create(1L, 10L));

        round.open();

        assertThat(round.getStatus()).isEqualTo(RecruitingRoundStatus.OPEN);
    }

    @Test
    @DisplayName("모집 차수는 OPEN에서 CLOSED로 전이한다")
    void roundClosesFromOpen() {
        RecruitingRound round = openRound();

        round.close();

        assertThat(round.getStatus()).isEqualTo(RecruitingRoundStatus.CLOSED);
    }

    @Test
    @DisplayName("모집 차수는 상태 전이 순서를 건너뛸 수 없다")
    void roundRejectsInvalidTransition() {
        RecruitingRound round = RecruitingRound.createRegular(RecruitingSeason.create(1L, 10L));

        assertThatThrownBy(round::close)
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_INVALID_TRANSITION);
    }

    private RecruitingSeason activeSeason() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        season.activate();
        return season;
    }

    private RecruitingRound openRound() {
        RecruitingRound round = RecruitingRound.createRegular(RecruitingSeason.create(1L, 10L));
        round.open();
        return round;
    }
}
