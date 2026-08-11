package com.umc.product.community.domain;

import java.time.Instant;

import com.umc.product.common.BaseEntity;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "community_thread_member",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_community_thread_member_thread_member",
        columnNames = {"thread_id", "member_id"}
    ),
    indexes = {
        @Index(
            name = "idx_community_thread_member_thread_state",
            columnList = "thread_id, state, role, member_id"
        ),
        @Index(
            name = "idx_community_thread_member_member_state",
            columnList = "member_id, state, is_pinned, thread_id"
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityThreadMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "thread_id", nullable = false)
    private Long threadId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityThreadMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityThreadMemberState state;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Column(name = "is_pinned", nullable = false)
    private boolean pinned;

    @Column(name = "is_muted", nullable = false)
    private boolean muted;

    @Column(name = "unread_count", nullable = false)
    private long unreadCount;

    private CommunityThreadMember(
        Long threadId,
        Long memberId,
        CommunityThreadMemberRole role,
        Instant joinedAt
    ) {
        this.threadId = requirePositive(threadId, "threadId");
        this.memberId = requirePositive(memberId, "memberId");
        this.role = requireRole(role);
        this.state = CommunityThreadMemberState.ACTIVE;
        this.joinedAt = requireInstant(joinedAt, "joinedAt");
    }

    public static CommunityThreadMember createOwner(Long threadId, Long memberId, Instant joinedAt) {
        return new CommunityThreadMember(threadId, memberId, CommunityThreadMemberRole.OWNER, joinedAt);
    }

    public static CommunityThreadMember createMember(Long threadId, Long memberId, Instant joinedAt) {
        return new CommunityThreadMember(threadId, memberId, CommunityThreadMemberRole.MEMBER, joinedAt);
    }

    public void changeRole(CommunityThreadMemberRole role) {
        this.role = requireRole(role);
    }

    public void leave(Instant leftAt) {
        requireActive();
        Instant validLeftAt = requireInstant(leftAt, "leftAt");
        this.state = CommunityThreadMemberState.LEFT;
        this.leftAt = validLeftAt;
    }

    public void kick(Instant kickedAt) {
        requireActive();
        Instant validKickedAt = requireInstant(kickedAt, "kickedAt");
        this.state = CommunityThreadMemberState.KICKED;
        this.leftAt = validKickedAt;
    }

    public void rejoin(Instant joinedAt) {
        if (state != CommunityThreadMemberState.LEFT) {
            throw new IllegalStateException("only LEFT members can rejoin");
        }
        Instant validJoinedAt = requireInstant(joinedAt, "joinedAt");
        this.state = CommunityThreadMemberState.ACTIVE;
        this.role = CommunityThreadMemberRole.MEMBER;
        this.joinedAt = validJoinedAt;
        this.leftAt = null;
        resetUnreadCount();
    }

    public boolean pin() {
        if (pinned) {
            return false;
        }
        pinned = true;
        return true;
    }

    public boolean unpin() {
        if (!pinned) {
            return false;
        }
        pinned = false;
        return true;
    }

    public boolean mute() {
        if (muted) {
            return false;
        }
        muted = true;
        return true;
    }

    public boolean unmute() {
        if (!muted) {
            return false;
        }
        muted = false;
        return true;
    }

    public void incrementUnreadCount() {
        unreadCount = Math.addExact(unreadCount, 1L);
    }

    public void updateUnreadCount(long unreadCount) {
        if (unreadCount < 0) {
            throw new IllegalArgumentException("unreadCount must not be negative");
        }
        this.unreadCount = unreadCount;
    }

    public void resetUnreadCount() {
        unreadCount = 0L;
    }

    public boolean isActive() {
        return state == CommunityThreadMemberState.ACTIVE;
    }

    /**
     * 강퇴된 멤버인지 판단한다.
     *
     * <p>LEFT와 달리 재초대할 수 없고, 스레드 목록/상세/메시지 조회가 모두 차단된다.</p>
     */
    public boolean isKicked() {
        return state == CommunityThreadMemberState.KICKED;
    }

    private void requireActive() {
        if (!isActive()) {
            throw new IllegalStateException("member must be ACTIVE");
        }
    }

    private static CommunityThreadMemberRole requireRole(CommunityThreadMemberRole role) {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        return role;
    }

    private static Long requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static Instant requireInstant(Instant value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }
}
