package com.umc.product.community.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import(CommunityThreadQueryRepository.class)
@DisplayName("CommunityThreadQueryRepository")
class CommunityThreadQueryRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    @Autowired
    CommunityThreadQueryRepository sut;

    @Autowired
    CommunityThreadRepository threadRepository;

    @Autowired
    CommunityThreadMemberRepository memberRepository;

    @Test
    @DisplayName("팬아웃 대상은 ACTIVE 멤버만 memberId 순서와 limit에 맞게 조회한다")
    void listActiveMemberIds_활성_멤버만_제한해_조회한다() {
        // given
        CommunityThread thread = threadRepository.save(createThread(110L));
        CommunityThreadMember owner = CommunityThreadMember.createOwner(thread.getId(), 30L, NOW);
        CommunityThreadMember active = CommunityThreadMember.createMember(thread.getId(), 10L, NOW);
        CommunityThreadMember left = CommunityThreadMember.createMember(thread.getId(), 20L, NOW);
        left.leave(NOW.plusSeconds(1));
        memberRepository.saveAll(List.of(owner, active, left));

        // when
        List<Long> memberIds = sut.listActiveMemberIdsByThreadId(thread.getId(), 1);

        // then
        assertThat(memberIds).containsExactly(10L);
    }

    @Test
    @DisplayName("초대 제외 대상은 ACTIVE와 KICKED이고 LEFT는 포함하지 않는다")
    void listInvitationBlockedMemberIds_left를_제외한다() {
        // given
        CommunityThread thread = threadRepository.save(createThread(111L));
        CommunityThreadMember active = CommunityThreadMember.createMember(thread.getId(), 10L, NOW);
        CommunityThreadMember left = CommunityThreadMember.createMember(thread.getId(), 20L, NOW);
        CommunityThreadMember kicked = CommunityThreadMember.createMember(thread.getId(), 30L, NOW);
        left.leave(NOW.plusSeconds(1));
        kicked.kick(NOW.plusSeconds(1));
        memberRepository.saveAll(List.of(active, left, kicked));

        // when
        List<Long> blockedMemberIds = sut.listInvitationBlockedMemberIds(thread.getId());

        // then
        assertThat(blockedMemberIds).containsExactly(10L, 30L);
    }

    @Test
    @DisplayName("팬아웃 조회 limit은 양수만 허용한다")
    void listActiveMemberIds_비양수_limit을_거절한다() {
        assertThatThrownBy(() -> sut.listActiveMemberIdsByThreadId(1L, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("limit must be positive");
    }

    private CommunityThread createThread(Long chatRoomId) {
        return CommunityThread.create(
            chatRoomId,
            "스레드",
            null,
            CommunityThreadCategory.STUDY,
            "📚",
            10L,
            NOW
        );
    }
}
