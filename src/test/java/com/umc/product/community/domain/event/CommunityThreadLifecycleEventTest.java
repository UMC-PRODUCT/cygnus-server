package com.umc.product.community.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.global.event.domain.OutboxDispatchMode;

@DisplayName("Community thread lifecycle event")
class CommunityThreadLifecycleEventTest {

    @Test
    @DisplayName("초대 ID snapshot은 정렬·중복 제거된 불변 값으로 보관한다")
    void invitedEvent_keepsImmutableSortedDistinctIds() {
        // given & when
        CommunityThreadInvitedEvent event = CommunityThreadInvitedEvent.of(
            1L,
            10L,
            List.of(30L, 20L, 30L),
            Instant.parse("2026-07-18T00:00:00Z")
        );

        // then
        assertThat(event.invitedMemberIds()).containsExactly(20L, 30L);
        assertThat(event.outboxDispatchMode()).isEqualTo(OutboxDispatchMode.NON_TRANSACTIONAL);
        assertThatThrownBy(() -> event.invitedMemberIds().add(40L))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
