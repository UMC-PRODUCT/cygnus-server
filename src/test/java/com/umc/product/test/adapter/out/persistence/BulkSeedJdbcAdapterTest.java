package com.umc.product.test.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.support.PersistenceAdapterTest;
import com.umc.product.test.application.port.out.dto.SeedChallengerRow;

@PersistenceAdapterTest
@ActiveProfiles("test")
class BulkSeedJdbcAdapterTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("벌크 저장한 Track 배열과 기존 Part를 챌린저로 다시 조회할 수 있다")
    void track_배열과_part_벌크_저장() {
        // Given
        Instant startsAt = Instant.parse("2026-09-01T00:00:00Z");
        Instant endsAt = Instant.parse("2027-02-28T00:00:00Z");
        Gisu partGisu = em.persist(Gisu.create(10L, startsAt, endsAt, false, GisuLearningType.PART));
        Gisu trackGisu = em.persist(Gisu.create(11L, startsAt, endsAt, false, GisuLearningType.TRACK));
        Member member = em.persist(Member.create("벌크회원", "벌크", "bulk-array@test.umc.local", null, null));
        em.flush();
        BulkSeedJdbcAdapter sut = new BulkSeedJdbcAdapter(jdbcTemplate);
        long partId = sut.currentMaxIds().challengerMaxId() + 1;
        long trackId = partId + 1;

        // When
        sut.insertChallengers(List.of(
            new SeedChallengerRow(partId, member.getId(), ChallengerPart.WEB, List.of(), partGisu.getId()),
            new SeedChallengerRow(trackId, member.getId(), null,
                List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER), trackGisu.getId())
        ));
        em.clear();

        // Then
        Challenger partChallenger = em.find(Challenger.class, partId);
        Challenger trackChallenger = em.find(Challenger.class, trackId);
        assertThat(partChallenger.getPart()).isEqualTo(ChallengerPart.WEB);
        assertThat(partChallenger.getTracks()).isEmpty();
        assertThat(trackChallenger.getPart()).isNull();
        assertThat(trackChallenger.getTracks()).containsExactly(ChallengerTrack.WEB_PRODUCT_ENGINEER);
    }
}
