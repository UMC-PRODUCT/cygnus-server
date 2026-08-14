package com.umc.product.demoday.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayEntryCode;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.DemodayStamp;
import com.umc.product.demoday.domain.DemodayVote;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.EntityManager;

@PersistenceAdapterTest
class DemodayPersistenceConstraintTest {

    private static final Instant OPENS_AT = Instant.parse("2026-07-27T09:00:00Z");
    private static final Instant CLOSES_AT = Instant.parse("2026-07-27T12:00:00Z");
    private static final Long MEMBER_ID = 100L;

    @Autowired
    private DemodayPollJpaRepository pollRepository;

    @Autowired
    private DemodayBoothJpaRepository boothRepository;

    @Autowired
    private DemodayEntryCodeJpaRepository entryCodeRepository;

    @Autowired
    private DemodayVoteJpaRepository voteRepository;

    @Autowired
    private DemodayStampJpaRepository stampRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("같은 투표에서 한 회원의 표를 중복 저장할 수 없다")
    void rejectDuplicateMemberVote() {
        // Given
        DemodayPoll poll = savePoll("회원 중복 투표");
        DemodayBooth firstBooth = saveBooth(poll, 1L);
        DemodayBooth secondBooth = saveBooth(poll, 2L);
        voteRepository.saveAndFlush(DemodayVote.forMember(poll.getId(), MEMBER_ID, firstBooth));

        // When & Then
        assertThatThrownBy(() -> voteRepository.saveAndFlush(
            DemodayVote.forMember(poll.getId(), MEMBER_ID, secondBooth)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("표의 회원 식별자와 입장 코드 식별자가 모두 없으면 저장할 수 없다")
    void rejectVoteWithoutVoterIdentifier() {
        // Given
        DemodayPoll poll = savePoll("투표 식별자 누락");
        DemodayBooth booth = saveBooth(poll, 1L);

        // When & Then
        assertThatThrownBy(() -> insertVote(poll.getId(), null, null, booth.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("표의 회원 식별자와 입장 코드 식별자를 동시에 저장할 수 없다")
    void rejectVoteWithBothVoterIdentifiers() {
        // Given
        DemodayPoll poll = savePoll("투표 식별자 중복");
        DemodayBooth booth = saveBooth(poll, 1L);
        DemodayEntryCode entryCode = saveEntryCode(poll, "b");

        // When & Then
        assertThatThrownBy(() -> insertVote(poll.getId(), MEMBER_ID, entryCode.getId(), booth.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("철회한 표도 같은 투표의 회원 중복 투표를 막는다")
    void rejectDuplicateMemberVoteAfterRevocation() {
        // Given
        DemodayPoll poll = savePoll("철회 후 중복 투표");
        DemodayBooth firstBooth = saveBooth(poll, 1L);
        DemodayBooth secondBooth = saveBooth(poll, 2L);
        DemodayVote revokedVote = DemodayVote.forMember(poll.getId(), MEMBER_ID, firstBooth);
        revokedVote.revoke(Instant.parse("2026-07-27T10:00:00Z"));
        voteRepository.saveAndFlush(revokedVote);

        // When & Then
        assertThatThrownBy(() -> voteRepository.saveAndFlush(
            DemodayVote.forMember(poll.getId(), MEMBER_ID, secondBooth)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 회원이 같은 부스의 스탬프를 중복 저장할 수 없다")
    void rejectDuplicateMemberStamp() {
        // Given
        DemodayPoll poll = savePoll("회원 중복 스탬프");
        DemodayBooth booth = saveBooth(poll, 1L);
        stampRepository.saveAndFlush(DemodayStamp.forMember(MEMBER_ID, booth));

        // When & Then
        assertThatThrownBy(() -> stampRepository.saveAndFlush(DemodayStamp.forMember(MEMBER_ID, booth)))
            .isInstanceOf(DataIntegrityViolationException.class);
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
        DemodayBooth reloadedBooth = boothRepository.findById(secondPollBooth.getId()).orElseThrow();

        // When & Then
        assertThatThrownBy(() -> DemodayVote.forMember(firstPoll.getId(), MEMBER_ID, reloadedBooth))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_VOTE_POLL_MISMATCH)
            );
    }

    @Test
    @DisplayName("영속화 후 다시 조회한 입장 코드와 부스의 투표가 다르면 스탬프를 생성할 수 없다")
    void rejectStampForBoothFromDifferentPollAfterReload() {
        // Given
        DemodayPoll firstPoll = savePoll("첫 번째 투표");
        DemodayPoll secondPoll = savePoll("두 번째 투표");
        DemodayEntryCode firstPollEntryCode = saveEntryCode(firstPoll, "c");
        DemodayBooth secondPollBooth = saveBooth(secondPoll, 2L);
        entityManager.flush();
        entityManager.clear();
        DemodayEntryCode reloadedEntryCode = entryCodeRepository.findById(firstPollEntryCode.getId()).orElseThrow();
        DemodayBooth reloadedBooth = boothRepository.findById(secondPollBooth.getId()).orElseThrow();

        // When & Then
        assertThatThrownBy(() -> DemodayStamp.forVisitor(reloadedEntryCode, reloadedBooth))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_STAMP_POLL_MISMATCH)
            );
    }

    private DemodayPoll savePoll(String name) {
        return pollRepository.saveAndFlush(DemodayPoll.create(9L, name, OPENS_AT, CLOSES_AT));
    }

    private DemodayBooth saveBooth(DemodayPoll poll, Long projectId) {
        return boothRepository.saveAndFlush(DemodayBooth.forProject(poll.getId(), projectId));
    }

    private DemodayEntryCode saveEntryCode(DemodayPoll poll, String seed) {
        return entryCodeRepository.saveAndFlush(
            DemodayEntryCode.create(poll.getId(), seed.repeat(DemodayEntryCode.HASH_LENGTH))
        );
    }

    private void insertVote(Long pollId, Long memberId, Long entryCodeId, Long boothId) {
        jdbcTemplate.update("""
            INSERT INTO demoday_vote (
                created_at,
                updated_at,
                demoday_poll_id,
                member_id,
                entry_code_id,
                target_booth_id
            ) VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?, ?)
            """, pollId, memberId, entryCodeId, boothId);
    }
}
