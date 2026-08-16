package com.umc.product.community.application.service.command;

import java.time.Instant;

import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;

final class CommunityThreadLifecycleTestFixtures {

    static final Long THREAD_ID = 1L;
    static final Long CHAT_ROOM_ID = 100L;
    static final Long GISU_ID = 5L;
    static final Long OWNER_ID = 10L;
    static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    private CommunityThreadLifecycleTestFixtures() {
    }

    static CommunityThread thread() {
        CommunityThread thread = CommunityThread.create(
            CHAT_ROOM_ID,
            "스레드",
            null,
            CommunityThreadCategory.FREE,
            "💬",
            OWNER_ID,
            NOW
        );
        ReflectionTestUtils.setField(thread, "id", THREAD_ID);
        return thread;
    }

    static CommunityThreadMember activeMember(Long memberId, CommunityThreadMemberRole role) {
        CommunityThreadMember member = role == CommunityThreadMemberRole.OWNER
            ? CommunityThreadMember.createOwner(THREAD_ID, memberId, NOW)
            : CommunityThreadMember.createMember(THREAD_ID, memberId, NOW);
        if (role == CommunityThreadMemberRole.ADMIN) {
            member.changeRole(CommunityThreadMemberRole.ADMIN);
        }
        ReflectionTestUtils.setField(member, "id", memberId + 1_000L);
        return member;
    }

    static CommunityThreadMember member(
        Long memberId,
        CommunityThreadMemberRole role,
        CommunityThreadMemberState state
    ) {
        CommunityThreadMember member = activeMember(memberId, role);
        if (state == CommunityThreadMemberState.LEFT) {
            member.leave(NOW.minusSeconds(60));
        } else if (state == CommunityThreadMemberState.KICKED) {
            member.kick(NOW.minusSeconds(60));
        }
        return member;
    }
}
