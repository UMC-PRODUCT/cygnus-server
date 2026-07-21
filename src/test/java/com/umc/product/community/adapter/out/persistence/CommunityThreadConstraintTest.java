package com.umc.product.community.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@DisplayName("CommunityThread PostgreSQL 제약")
class CommunityThreadConstraintTest {

    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    @Autowired
    CommunityThreadRepository threadRepository;

    @Autowired
    CommunityThreadMemberRepository memberRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("chatRoom은 스레드 전체에서 유일하다")
    void chatRoomUnique_중복을_거절한다() {
        // given
        threadRepository.saveAndFlush(createThread(120L));

        // when & then
        assertThatThrownBy(() -> threadRepository.saveAndFlush(createThread(120L)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("thread-member 조합은 유일하다")
    void threadMemberUnique_중복을_거절한다() {
        // given
        CommunityThread thread = threadRepository.saveAndFlush(createThread(123L));
        memberRepository.saveAndFlush(CommunityThreadMember.createOwner(thread.getId(), 40L, NOW));

        // when & then
        assertThatThrownBy(() -> memberRepository.saveAndFlush(
            CommunityThreadMember.createMember(thread.getId(), 40L, NOW)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("한 스레드에는 ACTIVE OWNER가 하나만 존재한다")
    void activeOwnerPartialUnique_두_번째_활성_소유자를_거절한다() {
        // given
        CommunityThread thread = threadRepository.save(createThread(121L));
        memberRepository.saveAndFlush(CommunityThreadMember.createOwner(thread.getId(), 41L, NOW));

        // when & then
        assertThatThrownBy(() -> memberRepository.saveAndFlush(
            CommunityThreadMember.createOwner(thread.getId(), 42L, NOW)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("기존 OWNER가 LEFT이면 새 ACTIVE OWNER를 저장할 수 있다")
    void activeOwnerPartialUnique_left_owner는_중복에서_제외한다() {
        // given
        CommunityThread thread = threadRepository.save(createThread(124L));
        CommunityThreadMember previousOwner = CommunityThreadMember.createOwner(thread.getId(), 44L, NOW);
        previousOwner.leave(NOW.plusSeconds(1));
        memberRepository.saveAndFlush(previousOwner);

        // when
        CommunityThreadMember currentOwner = memberRepository.saveAndFlush(
            CommunityThreadMember.createOwner(thread.getId(), 45L, NOW.plusSeconds(2))
        );

        // then
        assertThat(currentOwner.getId()).isPositive();
    }

    @Test
    @DisplayName("unread count 음수는 DB check 제약으로 거절한다")
    void unreadCountCheck_음수를_거절한다() {
        // given
        CommunityThread thread = threadRepository.save(createThread(122L));
        CommunityThreadMember member = memberRepository.saveAndFlush(
            CommunityThreadMember.createOwner(thread.getId(), 43L, NOW)
        );

        // when & then
        assertThatThrownBy(() -> jdbcTemplate.update(
            "UPDATE community_thread_member SET unread_count = -1 WHERE id = ?",
            member.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("THREAD_MESSAGE 신고는 threadId와 reasonCode가 모두 필요하다")
    void threadMessageReportCheck_필수_필드를_강제한다() {
        // when & then
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO report "
                + "(reporter_id, target_type, target_id, status, reason_code, created_at, updated_at) "
                + "VALUES (1, 'THREAD_MESSAGE', 2, 'PENDING', 'SPAM', now(), now())"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("기수 컬럼 없이 목록 멤버 초대 unread 인덱스와 도메인 내부 FK만 생성한다")
    void indexesAndForeignKeys_요구_경로만_지원한다() {
        // given & when
        Map<String, String> indexDefinitions = jdbcTemplate.query(
            "SELECT indexname, indexdef FROM pg_indexes "
                + "WHERE schemaname = 'public' AND tablename IN ('community_thread', 'community_thread_member')",
            resultSet -> {
                Map<String, String> definitions = new LinkedHashMap<>();
                while (resultSet.next()) {
                    definitions.put(resultSet.getString("indexname"), resultSet.getString("indexdef"));
                }
                return definitions;
            }
        );
        List<String> referencedTables = jdbcTemplate.queryForList(
            "SELECT referenced.relname "
                + "FROM pg_constraint constraint_definition "
                + "JOIN pg_class source ON source.oid = constraint_definition.conrelid "
                + "JOIN pg_class referenced ON referenced.oid = constraint_definition.confrelid "
                + "WHERE constraint_definition.contype = 'f' "
                + "AND source.relname IN ('community_thread', 'community_thread_member', 'report')",
            String.class
        );
        List<String> threadColumns = jdbcTemplate.queryForList(
            "SELECT column_name FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = 'community_thread'",
            String.class
        );

        // then
        assertThat(indexDefinitions).containsKeys(
            "idx_community_thread_active_list",
            "idx_community_thread_member_thread_state",
            "idx_community_thread_member_member_state",
            "idx_community_thread_member_member_unread",
            "uq_community_thread_member_active_owner"
        );
        assertThat(indexDefinitions.get("idx_community_thread_active_list"))
            .contains("(deleted_at, last_activity_at DESC, id DESC)");
        assertThat(threadColumns).doesNotContain("active_gisu_id");
        assertThat(indexDefinitions.get("idx_community_thread_member_thread_state"))
            .contains("(thread_id, state, role, member_id)");
        assertThat(indexDefinitions.get("idx_community_thread_member_member_state"))
            .contains("(member_id, state, is_pinned, thread_id)");
        assertThat(indexDefinitions.get("idx_community_thread_member_member_unread"))
            .contains(
                "(member_id, thread_id)",
                "WHERE",
                "(state)::text = 'ACTIVE'::text",
                "unread_count > 0"
            );
        assertThat(indexDefinitions.get("uq_community_thread_member_active_owner"))
            .contains(
                "CREATE UNIQUE INDEX",
                "(thread_id)",
                "WHERE",
                "(role)::text = 'OWNER'::text",
                "(state)::text = 'ACTIVE'::text"
            );
        assertThat(referencedTables).containsOnly("community_thread").hasSize(2);
    }

    private CommunityThread createThread(Long chatRoomId) {
        return CommunityThread.create(
            chatRoomId,
            "스레드",
            null,
            CommunityThreadCategory.FREE,
            "💬",
            40L,
            NOW
        );
    }
}
