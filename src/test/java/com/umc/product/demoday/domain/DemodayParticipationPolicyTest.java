package com.umc.product.demoday.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.demoday.domain.policy.DemodayParticipationPolicy;

@DisplayName("데모데이 참여 정책")
class DemodayParticipationPolicyTest {

    @Test
    @DisplayName("유효 스탬프가 6개 이상이면 필요 개수를 충족한다")
    void hasRequiredStamps() {
        assertThat(DemodayParticipationPolicy.requiredStampCount()).isEqualTo(6);
        assertThat(DemodayParticipationPolicy.hasRequiredStamps(5)).isFalse();
        assertThat(DemodayParticipationPolicy.hasRequiredStamps(6)).isTrue();
    }

    @Test
    @DisplayName("필요한 스탬프를 모으고 투표 슬롯을 사용하지 않아야 권한을 요청할 수 있다")
    void canRequestVoteAuthorization() {
        assertThat(DemodayParticipationPolicy.canRequestVoteAuthorization(5, false)).isFalse();
        assertThat(DemodayParticipationPolicy.canRequestVoteAuthorization(6, true)).isFalse();
        assertThat(DemodayParticipationPolicy.canRequestVoteAuthorization(6, false)).isTrue();
    }
}
