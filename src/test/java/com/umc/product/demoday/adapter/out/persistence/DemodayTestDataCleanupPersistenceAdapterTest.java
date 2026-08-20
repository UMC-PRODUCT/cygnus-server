package com.umc.product.demoday.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.demoday.application.port.out.DeleteDemodayTestDataPort;
import com.umc.product.demoday.application.port.out.dto.DemodayTestDataDeletionCounts;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayEntryCode;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.DemodayStamp;
import com.umc.product.demoday.domain.DemodayVote;
import com.umc.product.member.domain.Member;
import com.umc.product.member.domain.MemberSystemRole;
import com.umc.product.member.domain.MemberSystemRoleType;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@ActiveProfiles("test")
@TestPropertySource(properties = "demoday.test-data-reset.enabled=true")
@Import(DemodayTestDataCleanupPersistenceAdapter.class)
@DisplayName("DemodayTestDataCleanupPersistenceAdapter")
class DemodayTestDataCleanupPersistenceAdapterTest {

    private static final Instant OPENS_AT = Instant.parse("2026-08-21T09:00:00Z");
    private static final Instant CLOSES_AT = Instant.parse("2026-08-21T12:00:00Z");
    private static final Instant GISU_START_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant GISU_END_AT = Instant.parse("2026-12-31T00:00:00Z");

    @Autowired
    TestEntityManager em;

    @Autowired
    DeleteDemodayTestDataPort sut;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("데모데이 5개 테이블을 FK 역순으로 모두 삭제하고 정확한 건수를 반환한다")
    void deleteAllDemodayTestData() {
        // Given
        NonDemodaySentinels sentinels = persistNonDemodaySentinels();
        DemodayRows rows = persistDemodayRows(sentinels.gisuId(), sentinels.memberId());
        em.flush();
        em.clear();

        // When
        DemodayTestDataDeletionCounts result = sut.deleteAll();
        em.clear();

        // Then
        assertDeletionCounts(result, rows);
        assertDemodayTableCounts(TableCounts.empty());
        assertNonDemodaySentinelsExist(sentinels);
    }

    @Test
    @DisplayName("이미 비어 있는 데모데이 테스트 데이터 삭제를 반복하면"
        + " 모든 삭제 건수가 0이다")
    void returnZeroCountsWhenDeleteAllIsRepeated() {
        // Given
        DemodayRows rows = persistDemodayRows(9L, 100L);
        em.flush();
        em.clear();
        assertDeletionCounts(sut.deleteAll(), rows);

        // When
        DemodayTestDataDeletionCounts result = sut.deleteAll();

        // Then
        assertDeletionCounts(result, DemodayRows.empty());
        assertDemodayTableCounts(TableCounts.empty());
    }

    @Test
    @DisplayName("일반 DELETE는 데모데이 5개 테이블의 identity sequence를 초기화하지 않는다")
    void keepDemodayIdentitySequences() {
        // Given
        DemodayRows rows = persistDemodayRows(9L, 100L);
        em.flush();
        em.clear();
        sut.deleteAll();
        em.clear();

        // When
        DemodayPoll recreatedPoll = em.persist(createPoll(9L, "초기화 후 Poll"));
        DemodayBooth recreatedBooth = em.persist(DemodayBooth.forProject(recreatedPoll.getId(), 300L));
        DemodayEntryCode recreatedEntryCode = em.persist(DemodayEntryCode.create(recreatedPoll.getId(), hash('c')));
        DemodayVote recreatedVote = em.persist(DemodayVote.forVisitor(
            recreatedPoll.getId(),
            recreatedEntryCode,
            recreatedBooth
        ));
        DemodayStamp recreatedStamp = em.persist(DemodayStamp.forVisitor(recreatedEntryCode, recreatedBooth));
        em.flush();

        // Then
        assertThat(recreatedPoll.getId()).isGreaterThan(rows.maxPollId());
        assertThat(recreatedBooth.getId()).isGreaterThan(rows.maxBoothId());
        assertThat(recreatedEntryCode.getId()).isGreaterThan(rows.maxEntryCodeId());
        assertThat(recreatedStamp.getId()).isGreaterThan(rows.maxStampId());
        assertThat(recreatedVote.getId()).isGreaterThan(rows.maxVoteId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("삭제 도중 예외가 발생하면 데모데이 5개 테이블의 삭제를 모두 rollback한다")
    void rollbackAllDeletesWhenTransactionFails() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        DemodayRows rows = transaction.execute(status -> {
            DemodayRows persistedRows = persistDemodayRows(9L, 100L);
            em.flush();
            em.clear();
            return persistedRows;
        });

        try {
            // When & Then
            assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                sut.deleteAll();
                throw new IllegalStateException("데모데이 테스트 데이터 삭제 후 강제 실패");
            }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("데모데이 테스트 데이터 삭제 후 강제 실패");

            TableCounts actual = transaction.execute(status -> currentDemodayTableCounts());
            assertThat(actual).isEqualTo(rows.toTableCounts());
        } finally {
            transaction.executeWithoutResult(status -> {
                sut.deleteAll();
                em.clear();
            });
        }
    }

    private NonDemodaySentinels persistNonDemodaySentinels() {
        School school = em.persist(School.create("보존 대학교", "보존대", null));
        Gisu gisu = em.persist(Gisu.create(1300L, GISU_START_AT, GISU_END_AT, false));
        Member member = em.persist(Member.create(
            "보존회원",
            "보존회원",
            "demoday-reset-sentinel@test.com",
            school.getId(),
            null
        ));
        Challenger challenger = em.persist(new Challenger(
            member.getId(),
            ChallengerPart.SPRINGBOOT,
            gisu.getId()
        ));
        ChallengerRole challengerRole = em.persist(ChallengerRole.create(
            challenger.getId(),
            ChallengerRoleType.CENTRAL_PRESIDENT,
            null,
            null,
            gisu.getId()
        ));
        MemberSystemRole memberSystemRole = em.persist(MemberSystemRole.create(
            member.getId(),
            MemberSystemRoleType.SUPER_ADMIN
        ));
        Long auditLogId = insertAuditLogSentinel(member.getId());

        return new NonDemodaySentinels(
            auditLogId,
            school.getId(),
            gisu.getId(),
            member.getId(),
            challenger.getId(),
            challengerRole.getId(),
            memberSystemRole.getId()
        );
    }

    private Long insertAuditLogSentinel(Long actorMemberId) {
        Object id = em.getEntityManager().createNativeQuery("""
            INSERT INTO audit_log (
                domain, action, target_type, target_id, actor_member_id,
                description, details, ip_address, created_at
            )
            VALUES (
                'DEMODAY', 'CREATE', 'Sentinel', 'preserve-me', :actorMemberId,
                '데모데이 초기화 비대상 감사 로그', '{}'::jsonb, '127.0.0.1', now()
            )
            RETURNING id
            """)
            .setParameter("actorMemberId", actorMemberId)
            .getSingleResult();
        return ((Number) id).longValue();
    }

    private DemodayRows persistDemodayRows(Long gisuId, Long memberId) {
        DemodayPoll firstPoll = em.persist(createPoll(gisuId, "첫 번째 데모데이"));
        DemodayBooth firstBooth = em.persist(DemodayBooth.forProject(firstPoll.getId(), 100L));
        DemodayBooth secondBooth = em.persist(DemodayBooth.forExternal(firstPoll.getId(), "외부 부스"));
        DemodayEntryCode firstEntryCode = em.persist(DemodayEntryCode.create(firstPoll.getId(), hash('a')));
        em.persist(DemodayVote.forMember(firstPoll.getId(), memberId, firstBooth));
        em.persist(DemodayVote.forVisitor(firstPoll.getId(), firstEntryCode, secondBooth));
        em.persist(DemodayStamp.forMember(memberId, firstBooth));
        em.persist(DemodayStamp.forVisitor(firstEntryCode, secondBooth));

        DemodayPoll secondPoll = em.persist(createPoll(gisuId, "두 번째 데모데이"));
        DemodayBooth thirdBooth = em.persist(DemodayBooth.forProject(secondPoll.getId(), 200L));
        DemodayEntryCode secondEntryCode = em.persist(DemodayEntryCode.create(secondPoll.getId(), hash('b')));
        DemodayVote thirdVote = em.persist(DemodayVote.forVisitor(secondPoll.getId(), secondEntryCode, thirdBooth));
        DemodayStamp thirdStamp = em.persist(DemodayStamp.forVisitor(secondEntryCode, thirdBooth));

        return new DemodayRows(
            2,
            3,
            2,
            3,
            3,
            secondPoll.getId(),
            thirdBooth.getId(),
            secondEntryCode.getId(),
            thirdStamp.getId(),
            thirdVote.getId()
        );
    }

    private DemodayPoll createPoll(Long gisuId, String name) {
        return DemodayPoll.create(gisuId, name, OPENS_AT, CLOSES_AT);
    }

    private String hash(char value) {
        return String.valueOf(value).repeat(DemodayEntryCode.HASH_LENGTH);
    }

    private void assertDeletionCounts(DemodayTestDataDeletionCounts actual, DemodayRows expected) {
        assertThat(actual.deletedPolls()).isEqualTo(expected.polls());
        assertThat(actual.deletedBooths()).isEqualTo(expected.booths());
        assertThat(actual.deletedEntryCodes()).isEqualTo(expected.entryCodes());
        assertThat(actual.deletedStamps()).isEqualTo(expected.stamps());
        assertThat(actual.deletedVotes()).isEqualTo(expected.votes());
    }

    private void assertDemodayTableCounts(TableCounts expected) {
        assertThat(currentDemodayTableCounts()).isEqualTo(expected);
    }

    private TableCounts currentDemodayTableCounts() {
        return new TableCounts(
            countRows("demoday_poll"),
            countRows("demoday_booth"),
            countRows("demoday_entry_code"),
            countRows("demoday_stamp"),
            countRows("demoday_vote")
        );
    }

    private long countRows(String tableName) {
        Object result = em.getEntityManager()
            .createNativeQuery("SELECT COUNT(*) FROM " + tableName)
            .getSingleResult();
        return ((Number) result).longValue();
    }

    private void assertNonDemodaySentinelsExist(NonDemodaySentinels sentinels) {
        assertThat(em.find(School.class, sentinels.schoolId())).isNotNull();
        assertThat(em.find(Gisu.class, sentinels.gisuId())).isNotNull();
        assertThat(em.find(Member.class, sentinels.memberId())).isNotNull();
        assertThat(em.find(Challenger.class, sentinels.challengerId())).isNotNull();
        assertThat(em.find(ChallengerRole.class, sentinels.challengerRoleId())).isNotNull();
        assertThat(em.find(MemberSystemRole.class, sentinels.memberSystemRoleId())).isNotNull();
        assertThat(countRowsById("audit_log", sentinels.auditLogId())).isOne();
    }

    private long countRowsById(String tableName, Long id) {
        Object result = em.getEntityManager()
            .createNativeQuery("SELECT COUNT(*) FROM " + tableName + " WHERE id = :id")
            .setParameter("id", id)
            .getSingleResult();
        return ((Number) result).longValue();
    }

    private record NonDemodaySentinels(
        Long auditLogId,
        Long schoolId,
        Long gisuId,
        Long memberId,
        Long challengerId,
        Long challengerRoleId,
        Long memberSystemRoleId
    ) {
    }

    private record DemodayRows(
        long polls,
        long booths,
        long entryCodes,
        long stamps,
        long votes,
        Long maxPollId,
        Long maxBoothId,
        Long maxEntryCodeId,
        Long maxStampId,
        Long maxVoteId
    ) {

        private static DemodayRows empty() {
            return new DemodayRows(0, 0, 0, 0, 0, null, null, null, null, null);
        }

        private TableCounts toTableCounts() {
            return new TableCounts(polls, booths, entryCodes, stamps, votes);
        }
    }

    private record TableCounts(long polls, long booths, long entryCodes, long stamps, long votes) {

        private static TableCounts empty() {
            return new TableCounts(0, 0, 0, 0, 0);
        }
    }
}
