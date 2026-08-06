package com.umc.product.recruiting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

class RecruitingSeasonLifecycleTest {

    @Test
    @DisplayName("새 모집 시즌의 공유 메모는 비어 있다")
    void createdSeasonHasNoMemo() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);

        assertThat(season.getMemo()).isNull();
    }

    @Test
    @DisplayName("모집 시즌 공유 메모를 수정한다")
    void seasonUpdatesMemo() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);

        season.updateMemo("  운영진 공유 메모  ");

        assertThat(season.getMemo()).isEqualTo("운영진 공유 메모");
    }

    @Test
    @DisplayName("빈 모집 시즌 공유 메모는 null로 정규화한다")
    void seasonNormalizesBlankMemo() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L, "메모");

        season.updateMemo("   ");

        assertThat(season.getMemo()).isNull();
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

    private RecruitingRound openRound() {
        RecruitingRound round = RecruitingRound.createRegular(RecruitingSeason.create(1L, 10L));
        round.open();
        return round;
    }
}
