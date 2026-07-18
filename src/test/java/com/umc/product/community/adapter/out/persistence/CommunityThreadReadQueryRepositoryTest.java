package com.umc.product.community.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.umc.product.community.application.port.out.thread.dto.CommunityThreadListCondition;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadListRows;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadMemberRow;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.EntityManagerFactory;

@PersistenceAdapterTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(CommunityThreadQueryRepository.class)
@DisplayName("CommunityThreadQueryRepository 읽기 모델")
class CommunityThreadReadQueryRepositoryTest {

    private static final Long REQUESTER_ID = 100L;
    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    @Autowired
    CommunityThreadQueryRepository sut;

    @Autowired
    CommunityThreadRepository threadRepository;

    @Autowired
    CommunityThreadMemberRepository memberRepository;

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    Statistics statistics;

    @BeforeEach
    void setUpStatistics() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    @DisplayName("고정글 전체와 일반글 offset 페이지를 활동시각/ID 역순으로 분리하고 일반 total을 세 쿼리로 계산한다")
    void searchThreads_고정_일반_분리와_total을_고정_쿼리로_조회한다() {
        // given
        CommunityThread pinnedFirst = persistThread(1_001L, "고정 1", CommunityThreadCategory.STUDY);
        CommunityThread pinnedSecond = persistThread(1_002L, "고정 2", CommunityThreadCategory.STUDY);
        CommunityThread unpinnedFirst = persistThread(1_003L, "일반 1", CommunityThreadCategory.STUDY);
        CommunityThread unpinnedSecond = persistThread(1_004L, "일반 2", CommunityThreadCategory.STUDY);
        CommunityThread unpinnedThird = persistThread(1_005L, "일반 3", CommunityThreadCategory.STUDY);
        persistRequesterMembership(pinnedFirst, true, 0L);
        persistRequesterMembership(pinnedSecond, true, 0L);
        persistRequesterMembership(unpinnedFirst, false, 0L);
        persistRequesterMembership(unpinnedSecond, false, 0L);
        persistRequesterMembership(unpinnedThird, false, 0L);
        persistMembership(pinnedSecond, 200L);
        flushAndClear();

        // when
        CommunityThreadListRows result = sut.searchThreads(new CommunityThreadListCondition(
            REQUESTER_ID, null, false, null, 1, 1
        ));

        // then
        assertThat(result.pinned()).extracting(row -> row.threadId())
            .containsExactly(pinnedSecond.getId(), pinnedFirst.getId());
        assertThat(result.unpinned()).extracting(row -> row.threadId())
            .containsExactly(unpinnedSecond.getId());
        assertThat(result.unpinnedTotal()).isEqualTo(3L);
        assertThat(result.pinned().get(0).memberCount()).isEqualTo(2L);
        Set<Long> pinnedIds = new HashSet<>(result.pinned().stream().map(row -> row.threadId()).toList());
        assertThat(result.unpinned()).noneMatch(row -> pinnedIds.contains(row.threadId()));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("카테고리/키워드/안 읽음 필터와 ACTIVE 멤버십 및 비삭제 조건을 모두 적용한다")
    void searchThreads_필터와_접근_조건을_함께_적용한다() {
        // given
        CommunityThread visible = persistThread(1_011L, "Spring 질문", CommunityThreadCategory.QNA);
        CommunityThread read = persistThread(1_012L, "Spring 답변", CommunityThreadCategory.QNA);
        CommunityThread otherCategory = persistThread(1_013L, "Spring 스터디", CommunityThreadCategory.STUDY);
        CommunityThread deleted = persistThread(1_014L, "Spring 삭제", CommunityThreadCategory.QNA);
        CommunityThread outsider = persistThread(1_015L, "Spring 외부", CommunityThreadCategory.QNA);
        CommunityThread left = persistThread(1_016L, "Spring 탈퇴", CommunityThreadCategory.QNA);
        deleted.delete(NOW.plusSeconds(1));
        persistRequesterMembership(visible, false, 2L);
        persistRequesterMembership(read, false, 0L);
        persistRequesterMembership(otherCategory, false, 2L);
        persistRequesterMembership(deleted, false, 2L);
        persistMembership(outsider, 999L);
        CommunityThreadMember leftMembership = persistRequesterMembership(left, false, 2L);
        leftMembership.leave(NOW.plusSeconds(1));
        flushAndClear();

        // when
        CommunityThreadListRows result = sut.searchThreads(new CommunityThreadListCondition(
            REQUESTER_ID,
            CommunityThreadCategory.QNA,
            true,
            "spring",
            0,
            20
        ));

        // then
        assertThat(result.pinned()).isEmpty();
        assertThat(result.unpinned()).extracting(row -> row.threadId()).containsExactly(visible.getId());
        assertThat(result.unpinnedTotal()).isEqualTo(1L);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("멤버 읽기 projection은 ACTIVE 멤버만 ID 순서로 한 쿼리에 반환한다")
    void listActiveThreadMembers_ACTIVE만_한_쿼리로_조회한다() {
        // given
        CommunityThread thread = persistThread(1_021L, "멤버", CommunityThreadCategory.FREE);
        persistMembership(thread, 30L);
        persistMembership(thread, 10L);
        CommunityThreadMember left = persistMembership(thread, 20L);
        left.leave(NOW.plusSeconds(1));
        flushAndClear();

        // when
        List<CommunityThreadMemberRow> result = sut.listActiveThreadMembers(thread.getId());

        // then
        assertThat(result).extracting(CommunityThreadMemberRow::memberId).containsExactly(10L, 30L);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
    }

    private CommunityThread persistThread(
        Long chatRoomId,
        String title,
        CommunityThreadCategory category
    ) {
        return threadRepository.save(CommunityThread.create(
            chatRoomId,
            title,
            null,
            category,
            "💬",
            REQUESTER_ID,
            20L,
            NOW
        ));
    }

    private CommunityThreadMember persistRequesterMembership(
        CommunityThread thread,
        boolean pinned,
        long unreadCount
    ) {
        CommunityThreadMember member = CommunityThreadMember.createOwner(thread.getId(), REQUESTER_ID, NOW);
        if (pinned) {
            member.pin();
        }
        member.updateUnreadCount(unreadCount);
        return memberRepository.save(member);
    }

    private CommunityThreadMember persistMembership(CommunityThread thread, Long memberId) {
        return memberRepository.save(CommunityThreadMember.createMember(thread.getId(), memberId, NOW));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
        statistics.clear();
    }
}
