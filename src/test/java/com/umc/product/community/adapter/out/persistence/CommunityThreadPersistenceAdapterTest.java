package com.umc.product.community.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({CommunityThreadPersistenceAdapter.class, CommunityThreadQueryRepository.class})
@DisplayName("CommunityThreadPersistenceAdapter")
class CommunityThreadPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    @Autowired
    CommunityThreadPersistenceAdapter sut;

    @Autowired
    CommunityThreadRepository threadRepository;

    @Autowired
    CommunityThreadMemberRepository memberRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    @DisplayName("스레드와 멤버 직접 JPA 도메인을 저장하고 scalar ID로 조회한다")
    void saveAndFind_스레드와_멤버를_저장_조회한다() {
        // given
        CommunityThread thread = createThread(100L);
        thread.updateLastMessage(300L, "마지막 메시지", 200L, NOW.plusSeconds(1));
        thread.delete(NOW.plusSeconds(2));
        thread = sut.save(thread);
        CommunityThreadMember owner = CommunityThreadMember.createOwner(thread.getId(), 200L, NOW);
        owner.pin();
        owner.mute();
        owner.updateUnreadCount(2L);

        // when
        sut.save(owner);
        threadRepository.flush();
        memberRepository.flush();
        Long threadId = thread.getId();
        entityManager.clear();

        // then
        CommunityThread reloadedThread = sut.findById(threadId).orElseThrow();
        CommunityThreadMember reloadedOwner = sut.findByThreadIdAndMemberId(threadId, 200L).orElseThrow();
        assertThat(reloadedThread.getChatRoomId()).isEqualTo(100L);
        assertThat(reloadedThread.getLastMessageId()).isEqualTo(300L);
        assertThat(reloadedThread.getLastMessagePreview()).isEqualTo("마지막 메시지");
        assertThat(reloadedThread.getLastActivityAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(reloadedThread.getDeletedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(sut.findByChatRoomId(100L)).contains(reloadedThread);
        assertThat(reloadedOwner.getRole()).isEqualTo(owner.getRole());
        assertThat(reloadedOwner.isPinned()).isTrue();
        assertThat(reloadedOwner.isMuted()).isTrue();
        assertThat(reloadedOwner.getUnreadCount()).isEqualTo(2L);
        assertThat(sut.listByThreadId(threadId)).containsExactly(reloadedOwner);
    }

    @Test
    @DisplayName("여러 멤버를 일괄 저장하고 ACTIVE 멤버 수를 조회한다")
    void saveAllAndCount_활성_멤버_수를_조회한다() {
        // given
        CommunityThread thread = sut.save(createThread(101L));
        CommunityThreadMember owner = CommunityThreadMember.createOwner(thread.getId(), 201L, NOW);
        CommunityThreadMember active = CommunityThreadMember.createMember(thread.getId(), 202L, NOW);
        CommunityThreadMember left = CommunityThreadMember.createMember(thread.getId(), 203L, NOW);
        left.leave(NOW.plusSeconds(1));

        // when
        sut.saveAll(List.of(owner, active, left));

        // then
        assertThat(sut.countActiveByThreadId(thread.getId())).isEqualTo(2L);
    }

    @Test
    @DisplayName("소유권 이전은 기존 OWNER를 먼저 내린 뒤 새 OWNER를 올려 ACTIVE OWNER를 하나만 유지한다")
    void transferOwnership_활성_OWNER를_하나만_유지한다() {
        // given
        CommunityThread thread = sut.save(createThread(102L));
        CommunityThreadMember owner = CommunityThreadMember.createOwner(thread.getId(), 201L, NOW);
        CommunityThreadMember target = CommunityThreadMember.createMember(thread.getId(), 202L, NOW);
        sut.saveAll(List.of(owner, target));
        memberRepository.flush();
        entityManager.clear();

        // when
        sut.transferOwnership(thread.getId(), 201L, 202L);
        memberRepository.flush();
        entityManager.clear();

        // then
        List<CommunityThreadMember> members = sut.listByThreadId(thread.getId());
        assertThat(members)
            .filteredOn(member -> member.getMemberId().equals(201L))
            .extracting(CommunityThreadMember::getRole)
            .containsExactly(CommunityThreadMemberRole.ADMIN);
        assertThat(members)
            .filteredOn(member -> member.getMemberId().equals(202L))
            .extracting(CommunityThreadMember::getRole)
            .containsExactly(CommunityThreadMemberRole.OWNER);
        assertThat(members)
            .filteredOn(member -> member.isActive()
                && member.getRole() == CommunityThreadMemberRole.OWNER)
            .hasSize(1);
    }

    private CommunityThread createThread(Long chatRoomId) {
        return CommunityThread.create(
            chatRoomId,
            "스레드",
            "설명",
            CommunityThreadCategory.FREE,
            "💬",
            200L,
            300L,
            NOW
        );
    }
}
