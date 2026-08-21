package com.umc.product.demoday.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.umc.product.demoday.application.port.out.DemodayVoteSearchCondition;
import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayVotePort;
import com.umc.product.demoday.application.port.out.SaveDemodayBoothPort;
import com.umc.product.demoday.application.port.out.SaveDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.SaveDemodayPollPort;
import com.umc.product.demoday.application.port.out.SaveDemodayVotePort;
import com.umc.product.demoday.application.port.out.SearchDemodayVotePort;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayEntryCode;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.DemodayVote;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.EntityManager;

@PersistenceAdapterTest
@Import({
    DemodayPollPersistenceAdapter.class,
    DemodayBoothPersistenceAdapter.class,
    DemodayEntryCodePersistenceAdapter.class,
    DemodayVotePersistenceAdapter.class,
    DemodayVoteQueryRepository.class
})
class DemodayVotePersistenceAdapterTest {

    private static final Instant OPENS_AT = Instant.parse("2026-07-27T09:00:00Z");
    private static final Instant CLOSES_AT = Instant.parse("2026-07-27T12:00:00Z");
    private static final Long MEMBER_ID = 100L;

    @Autowired
    private LoadDemodayVotePort loadDemodayVotePort;

    @Autowired
    private SaveDemodayVotePort saveDemodayVotePort;

    @Autowired
    private SearchDemodayVotePort searchDemodayVotePort;

    @Autowired
    private SaveDemodayPollPort saveDemodayPollPort;

    @Autowired
    private SaveDemodayBoothPort saveDemodayBoothPort;

    @Autowired
    private LoadDemodayBoothPort loadDemodayBoothPort;

    @Autowired
    private SaveDemodayEntryCodePort saveDemodayEntryCodePort;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("회원 표를 저장하고 식별자와 투표·회원 조건으로 조회한다")
    void saveAndFindMemberVote() {
        // Given
        DemodayPoll poll = savePoll("회원 표 조회");
        DemodayBooth booth = saveBooth(poll, 1L);
        DemodayVote savedVote = saveDemodayVotePort.save(
            DemodayVote.forMember(poll.getId(), MEMBER_ID, booth)
        );

        entityManager.clear();

        // When & Then
        assertThat(loadDemodayVotePort.findById(savedVote.getId()))
            .get()
            .extracting(DemodayVote::getId)
            .isEqualTo(savedVote.getId());
        assertThat(loadDemodayVotePort.findMemberVote(poll.getId(), MEMBER_ID))
            .get()
            .extracting(DemodayVote::getId)
            .isEqualTo(savedVote.getId());
    }

    @Test
    @DisplayName("방문자 표를 저장하고 입장 코드 조건으로 조회한다")
    void saveAndFindVisitorVote() {
        // Given
        DemodayPoll poll = savePoll("방문자 표 조회");
        DemodayBooth booth = saveBooth(poll, 1L);
        DemodayEntryCode entryCode = saveEntryCode(poll, "a");
        DemodayVote savedVote = saveDemodayVotePort.save(
            DemodayVote.forVisitor(poll.getId(), entryCode, booth)
        );

        entityManager.clear();

        // When & Then
        assertThat(loadDemodayVotePort.findVisitorVote(entryCode.getId()))
            .get()
            .extracting(DemodayVote::getId)
            .isEqualTo(savedVote.getId());
    }

    @Test
    @DisplayName("투표의 표 목록을 최근 저장된 순서로 조회한다")
    void listVotesInRecentOrder() {
        // Given
        DemodayPoll poll = savePoll("표 목록 정렬");
        List<DemodayBooth> booths = saveDemodayBoothPort.saveAll(List.of(
            DemodayBooth.forProject(poll.getId(), 1, 1L),
            DemodayBooth.forProject(poll.getId(), 2, 2L)
        ));
        DemodayVote firstVote = saveDemodayVotePort.save(
            DemodayVote.forMember(poll.getId(), MEMBER_ID, booths.get(0))
        );
        DemodayEntryCode entryCode = saveEntryCode(poll, "b");
        DemodayVote secondVote = saveDemodayVotePort.save(
            DemodayVote.forVisitor(poll.getId(), entryCode, booths.get(1))
        );

        entityManager.clear();

        // When
        List<DemodayVote> votes = loadDemodayVotePort.listVotes(poll.getId());

        // Then
        assertThat(votes)
            .extracting(DemodayVote::getId)
            .containsExactly(secondVote.getId(), firstVote.getId());
    }

    @Test
    @DisplayName("관리자 검색은 무효 표를 포함하고 부스·커서를 AND로 적용한다")
    void searchAdminVotesWithBoothAndCursor() {
        // Given
        DemodayPoll poll = savePoll("관리자 표 검색");
        DemodayBooth firstBooth = saveBooth(poll, 1L);
        DemodayBooth secondBooth = saveBooth(poll, 2L);
        DemodayVote firstVote = saveDemodayVotePort.save(
            DemodayVote.forMember(poll.getId(), MEMBER_ID, firstBooth)
        );
        DemodayEntryCode entryCode = saveEntryCode(poll, "c");
        DemodayVote revokedVote = DemodayVote.forVisitor(poll.getId(), entryCode, firstBooth);
        revokedVote.revoke(Instant.parse("2026-07-27T10:00:00Z"));
        revokedVote = saveDemodayVotePort.save(revokedVote);
        DemodayEntryCode otherEntryCode = saveEntryCode(poll, "d");
        saveDemodayVotePort.save(DemodayVote.forVisitor(poll.getId(), otherEntryCode, secondBooth));

        entityManager.flush();
        entityManager.clear();

        // When
        List<DemodayVote> firstPage = searchDemodayVotePort.search(
            new DemodayVoteSearchCondition(poll.getId(), null, firstBooth.getId(), null, 10)
        );
        List<DemodayVote> afterCursor = searchDemodayVotePort.search(
            new DemodayVoteSearchCondition(poll.getId(), revokedVote.getId(), firstBooth.getId(), null, 10)
        );

        // Then
        assertThat(firstPage)
            .extracting(DemodayVote::getId)
            .containsExactly(revokedVote.getId(), firstVote.getId());
        assertThat(firstPage.get(0).isRevoked()).isTrue();
        assertThat(afterCursor)
            .extracting(DemodayVote::getId)
            .containsExactly(firstVote.getId());
        assertThat(searchDemodayVotePort.listMemberIds(poll.getId(), firstBooth.getId()))
            .containsExactly(MEMBER_ID);
    }

    @Test
    @DisplayName("Poll과 표 ID를 함께 잠금 조회해 다른 Poll의 표를 숨긴다")
    void loadVoteForUpdateWithinPoll() {
        // Given
        DemodayPoll poll = savePoll("잠금 조회 대상");
        DemodayPoll otherPoll = savePoll("다른 투표");
        DemodayBooth booth = saveBooth(poll, 1L);
        DemodayVote vote = saveDemodayVotePort.save(DemodayVote.forMember(poll.getId(), MEMBER_ID, booth));
        entityManager.flush();
        entityManager.clear();

        // When & Then
        assertThat(loadDemodayVotePort.findByIdInPollForUpdate(poll.getId(), vote.getId())).isPresent();
        assertThat(loadDemodayVotePort.findByIdInPollForUpdate(otherPoll.getId(), vote.getId())).isEmpty();
    }

    @Test
    @DisplayName("영속화 후 다시 조회한 다른 투표의 부스에는 표를 생성할 수 없다")
    void rejectVoteForBoothFromDifferentPollAfterReload() {
        // Given
        DemodayPoll firstPoll = savePoll("첫 번째 투표");
        DemodayPoll secondPoll = savePoll("두 번째 투표");
        DemodayBooth secondPollBooth = saveBooth(secondPoll, 2L);
        entityManager.flush();
        entityManager.clear();
        DemodayBooth reloadedBooth = loadDemodayBoothPort.findById(secondPollBooth.getId()).orElseThrow();

        // When & Then
        assertThatThrownBy(() -> DemodayVote.forMember(firstPoll.getId(), MEMBER_ID, reloadedBooth))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_VOTE_POLL_MISMATCH)
            );
    }

    private DemodayPoll savePoll(String name) {
        return saveDemodayPollPort.save(DemodayPoll.create(9L, name, OPENS_AT, CLOSES_AT));
    }

    private DemodayBooth saveBooth(DemodayPoll poll, Long projectId) {
        return saveDemodayBoothPort.save(
            DemodayBooth.forProject(poll.getId(), Math.toIntExact(projectId), projectId)
        );
    }

    private DemodayEntryCode saveEntryCode(DemodayPoll poll, String seed) {
        return saveDemodayEntryCodePort.save(
            DemodayEntryCode.create(poll.getId(), seed.repeat(DemodayEntryCode.HASH_LENGTH))
        );
    }
}
