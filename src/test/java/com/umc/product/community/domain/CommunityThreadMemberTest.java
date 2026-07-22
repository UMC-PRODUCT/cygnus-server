package com.umc.product.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;

@DisplayName("CommunityThreadMember")
class CommunityThreadMemberTest {

    private static final Instant JOINED_AT = Instant.parse("2026-07-18T00:00:00Z");

    @Test
    @DisplayName("소유자는 ACTIVE 상태와 기본 설정으로 생성된다")
    void createOwner_active_기본값으로_생성한다() {
        // given & when
        CommunityThreadMember owner = CommunityThreadMember.createOwner(1L, 10L, JOINED_AT);

        // then
        assertThat(owner.getRole()).isEqualTo(CommunityThreadMemberRole.OWNER);
        assertThat(owner.getState()).isEqualTo(CommunityThreadMemberState.ACTIVE);
        assertThat(owner.getJoinedAt()).isEqualTo(JOINED_AT);
        assertThat(owner.getLeftAt()).isNull();
        assertThat(owner.isPinned()).isFalse();
        assertThat(owner.isMuted()).isFalse();
        assertThat(owner.getUnreadCount()).isZero();
    }

    @Test
    @DisplayName("LEFT 재가입은 설정을 유지하고 과거 unread를 초기화한다")
    void settingsAndRejoin_상태를_명시적으로_전이한다() {
        // given
        CommunityThreadMember member = CommunityThreadMember.createMember(1L, 11L, JOINED_AT);
        Instant leftAt = JOINED_AT.plusSeconds(10);
        Instant rejoinedAt = JOINED_AT.plusSeconds(20);

        // when
        member.pin();
        member.mute();
        member.incrementUnreadCount();
        member.leave(leftAt);
        member.rejoin(rejoinedAt);

        // then
        assertThat(member.isPinned()).isTrue();
        assertThat(member.isMuted()).isTrue();
        assertThat(member.getUnreadCount()).isZero();
        assertThat(member.getState()).isEqualTo(CommunityThreadMemberState.ACTIVE);
        assertThat(member.getRole()).isEqualTo(CommunityThreadMemberRole.MEMBER);
        assertThat(member.getJoinedAt()).isEqualTo(rejoinedAt);
        assertThat(member.getLeftAt()).isNull();
    }

    @Test
    @DisplayName("KICKED 멤버 재가입과 음수 unread projection을 거절한다")
    void invalidTransitions_거절한다() {
        // given
        CommunityThreadMember member = CommunityThreadMember.createMember(1L, 11L, JOINED_AT);
        member.kick(JOINED_AT.plusSeconds(10));

        // when & then
        assertThatThrownBy(() -> member.rejoin(JOINED_AT.plusSeconds(20)))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> member.updateUnreadCount(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("탈퇴 시각이 없으면 ACTIVE 상태를 유지한다")
    void leave_시각을_거절하면_상태를_유지한다() {
        // given
        CommunityThreadMember member = CommunityThreadMember.createMember(1L, 11L, JOINED_AT);

        // when & then
        assertThatThrownBy(() -> member.leave(null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(member.getState()).isEqualTo(CommunityThreadMemberState.ACTIVE);
        assertThat(member.getJoinedAt()).isEqualTo(JOINED_AT);
        assertThat(member.getLeftAt()).isNull();
    }

    @Test
    @DisplayName("강퇴 시각이 없으면 ACTIVE 상태를 유지한다")
    void kick_시각을_거절하면_상태를_유지한다() {
        // given
        CommunityThreadMember member = CommunityThreadMember.createMember(1L, 11L, JOINED_AT);

        // when & then
        assertThatThrownBy(() -> member.kick(null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(member.getState()).isEqualTo(CommunityThreadMemberState.ACTIVE);
        assertThat(member.getJoinedAt()).isEqualTo(JOINED_AT);
        assertThat(member.getLeftAt()).isNull();
    }

    @Test
    @DisplayName("재가입 시각이 없으면 LEFT 상태와 역할을 유지한다")
    void rejoin_시각을_거절하면_상태와_역할을_유지한다() {
        // given
        CommunityThreadMember owner = CommunityThreadMember.createOwner(1L, 10L, JOINED_AT);
        Instant leftAt = JOINED_AT.plusSeconds(10);
        owner.leave(leftAt);

        // when & then
        assertThatThrownBy(() -> owner.rejoin(null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(owner.getState()).isEqualTo(CommunityThreadMemberState.LEFT);
        assertThat(owner.getRole()).isEqualTo(CommunityThreadMemberRole.OWNER);
        assertThat(owner.getJoinedAt()).isEqualTo(JOINED_AT);
        assertThat(owner.getLeftAt()).isEqualTo(leftAt);
    }

    @Test
    @DisplayName("pin·mute 전이는 멱등이고 반대 전이는 실제 상태 변경만 보고한다")
    void pinAndMuteAreIdempotent() {
        CommunityThreadMember member = CommunityThreadMember.createMember(1L, 11L, JOINED_AT);

        member.changeRole(CommunityThreadMemberRole.ADMIN);
        member.updateUnreadCount(3);
        assertThat(member.getRole()).isEqualTo(CommunityThreadMemberRole.ADMIN);
        assertThat(member.getUnreadCount()).isEqualTo(3);

        assertThat(member.pin()).isTrue();
        assertThat(member.pin()).isFalse();
        assertThat(member.unpin()).isTrue();
        assertThat(member.unpin()).isFalse();
        assertThat(member.mute()).isTrue();
        assertThat(member.mute()).isFalse();
        assertThat(member.unmute()).isTrue();
        assertThat(member.unmute()).isFalse();
    }

    @Test
    @DisplayName("비활성 재전이·null role·non-positive ID를 거부한다")
    void rejectsInvalidStateAndIdentityTransitions() {
        CommunityThreadMember member = CommunityThreadMember.createMember(1L, 11L, JOINED_AT);
        member.leave(JOINED_AT.plusSeconds(1));

        assertThatThrownBy(() -> member.leave(JOINED_AT.plusSeconds(2)))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> member.changeRole(null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CommunityThreadMember.createMember(0L, 1L, JOINED_AT))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CommunityThreadMember.createMember(1L, 0L, JOINED_AT))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
