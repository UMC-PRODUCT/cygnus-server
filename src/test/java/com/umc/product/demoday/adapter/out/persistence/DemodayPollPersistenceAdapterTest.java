package com.umc.product.demoday.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.SaveDemodayPollPort;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.EntityManager;

@PersistenceAdapterTest
@Import(DemodayPollPersistenceAdapter.class)
class DemodayPollPersistenceAdapterTest {

    private static final Instant OPENS_AT = Instant.parse("2026-07-27T09:00:00Z");
    private static final Instant CLOSES_AT = Instant.parse("2026-07-27T12:00:00Z");

    @Autowired
    private LoadDemodayPollPort loadDemodayPollPort;

    @Autowired
    private SaveDemodayPollPort saveDemodayPollPort;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("데모데이 투표를 저장한 뒤 식별자로 조회한다")
    void saveAndFindPollById() {
        // Given
        DemodayPoll poll = savePoll("9기 데모데이", OPENS_AT);
        clearPersistenceContext();

        // When
        DemodayPoll found = loadDemodayPollPort.findById(poll.getId()).orElseThrow();

        // Then
        assertThat(found.getId()).isEqualTo(poll.getId());
    }

    @Test
    @DisplayName("데모데이 투표 목록을 시작 시각과 식별자의 내림차순으로 조회한다")
    void listPollsByOpensAtAndIdDescending() {
        // Given
        DemodayPoll first = savePoll("첫 번째 투표", OPENS_AT);
        DemodayPoll second = savePoll("두 번째 투표", OPENS_AT);
        DemodayPoll latest = savePoll("가장 늦은 투표", OPENS_AT.plusSeconds(3600));
        clearPersistenceContext();

        // When
        List<DemodayPoll> polls = loadDemodayPollPort.listAll();

        // Then
        assertThat(polls)
            .extracting(DemodayPoll::getId)
            .containsExactly(latest.getId(), second.getId(), first.getId());
    }

    private DemodayPoll savePoll(String name, Instant opensAt) {
        return saveDemodayPollPort.save(DemodayPoll.create(9L, name, opensAt, CLOSES_AT.plusSeconds(3600)));
    }

    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
