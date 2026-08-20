package com.umc.product.demoday.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.umc.product.support.PersistenceAdapterTest;

/**
 * 엔티티 매핑이 마이그레이션 DDL과 실제로 맞는지 검증한다.
 *
 * <p>도메인 단위 테스트는 Hibernate를 띄우지 않아 매핑 오류를 잡지 못한다. 특히
 * {@link DemodayPoll#getBooths()}는 FK를 소유하지 않는 읽기 전용 @OneToMany라
 * 컬럼 중복 매핑이나 조인 테이블 생성 같은 실수가 부팅 시점에만 드러난다.
 */
@PersistenceAdapterTest
@DisplayName("DemodayEntityMappingTest")
class DemodayEntityMappingTest {

    private static final Long GISU_ID = 8L;
    private static final Long MEMBER_ID = 100L;
    private static final Long PROJECT_ID = 200L;

    @Autowired
    TestEntityManager em;

    private DemodayPoll persistPoll() {
        DemodayPoll poll = DemodayPoll.create(
            GISU_ID,
            "8기 데모데이",
            Instant.parse("2026-08-01T05:00:00Z"),
            Instant.parse("2026-08-01T08:00:00Z")
        );
        return em.persistAndFlush(poll);
    }

    @Test
    @DisplayName("부스는 자기 pollId로 FK를 쓰고, 투표는 그 부스들을 읽기 전용 컬렉션으로 다시 읽는다.")
    void mapBoothCollection() {
        // given
        DemodayPoll poll = persistPoll();
        em.persistAndFlush(DemodayBooth.forProject(poll.getId(), 11, PROJECT_ID));
        em.persistAndFlush(DemodayBooth.forExternal(poll.getId(), 12, "외부 부스"));

        // when
        em.clear(); //현재 영속성 컨텍스트(1차 캐시)를 비운다.
        DemodayPoll reloaded = em.find(DemodayPoll.class, poll.getId());

        // then
        assertThat(reloaded.getBooths())
            .hasSize(2)
            .allSatisfy(booth -> assertThat(booth.getPollId()).isEqualTo(poll.getId()))
            .extracting(DemodayBooth::getDisplayName)
            .containsExactlyInAnyOrder(null, "외부 부스");
    }

    @Test
    @DisplayName("표와 스탬프는 식별자 참조만으로 저장되고 다시 읽어온다.")
    void mapVoteAndStamp() {
        // given
        DemodayPoll poll = persistPoll();
        DemodayBooth booth = em.persistAndFlush(DemodayBooth.forProject(poll.getId(), 11, PROJECT_ID));
        DemodayEntryCode entryCode = em.persistAndFlush(DemodayEntryCode.create(poll.getId(), "code-hash"));

        DemodayVote memberVote = em.persistAndFlush(DemodayVote.forMember(poll.getId(), MEMBER_ID, booth));
        DemodayVote visitorVote = em.persistAndFlush(DemodayVote.forVisitor(poll.getId(), entryCode, booth));
        DemodayStamp stamp = em.persistAndFlush(DemodayStamp.forVisitor(entryCode, booth));

        // when
        em.clear();

        // then
        DemodayVote reloadedMemberVote = em.find(DemodayVote.class, memberVote.getId());
        assertThat(reloadedMemberVote.getPollId()).isEqualTo(poll.getId());
        assertThat(reloadedMemberVote.getTargetBoothId()).isEqualTo(booth.getId());
        assertThat(reloadedMemberVote.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(reloadedMemberVote.getEntryCodeId()).isNull();

        DemodayVote reloadedVisitorVote = em.find(DemodayVote.class, visitorVote.getId());
        assertThat(reloadedVisitorVote.getEntryCodeId()).isEqualTo(entryCode.getId());
        assertThat(reloadedVisitorVote.getMemberId()).isNull();

        DemodayStamp reloadedStamp = em.find(DemodayStamp.class, stamp.getId());
        assertThat(reloadedStamp.getBoothId()).isEqualTo(booth.getId());
        assertThat(reloadedStamp.getEntryCodeId()).isEqualTo(entryCode.getId());
    }
}
