package com.umc.product.recruiting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

class RecruitingSeasonTrackQuotaTest {

    @Test
    @DisplayName("시즌 트랙 쿼터는 목표 인원 0명으로 생성할 수 있다")
    void quotaAcceptsZeroTargetCountOnCreate() {
        RecruitingSeasonTrackQuota quota = RecruitingSeasonTrackQuota.create(
            RecruitingSeason.create(1L, 10L),
            ChallengerTrack.PLAN,
            0
        );

        assertThat(quota.getTrack()).isEqualTo(ChallengerTrack.PLAN);
        assertThat(quota.getTargetCount()).isZero();
    }

    @Test
    @DisplayName("시즌 트랙 쿼터 목표 인원을 변경할 수 있다")
    void quotaUpdatesTargetCount() {
        RecruitingSeasonTrackQuota quota = RecruitingSeasonTrackQuota.create(
            RecruitingSeason.create(1L, 10L),
            ChallengerTrack.PLAN,
            0
        );

        quota.updateTargetCount(10);

        assertThat(quota.getTargetCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("시즌 트랙 쿼터는 음수 목표 인원을 거부한다")
    void quotaRejectsNegativeTargetCount() {
        assertThatThrownBy(() -> RecruitingSeasonTrackQuota.create(
            RecruitingSeason.create(1L, 10L),
            ChallengerTrack.PLAN,
            -1
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_QUOTA_INVALID_TARGET_COUNT);
    }

    @Test
    @DisplayName("시즌 쿼터에는 INFRA_PLUS 트랙을 설정할 수 없다")
    void quotaRejectsInfraPlus() {
        assertThatThrownBy(() -> RecruitingSeasonTrackQuota.create(
            RecruitingSeason.create(1L, 10L),
            ChallengerTrack.INFRA_PLUS,
            1
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_QUOTA_UNSUPPORTED_TRACK);
    }
}
