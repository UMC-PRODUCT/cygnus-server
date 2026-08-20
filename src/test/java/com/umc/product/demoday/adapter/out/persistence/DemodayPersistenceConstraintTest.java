package com.umc.product.demoday.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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
import com.umc.product.support.PersistenceAdapterTest;

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

    @Test
    @DisplayName("같은 투표에서 부스 코드를 중복 저장할 수 없다")
    void rejectDuplicateBoothCodeInSamePoll() {
        // Given
        DemodayPoll poll = savePoll("부스 코드 중복");
        saveBooth(poll, 11, 1L);

        // When & Then
        assertConstraintViolation(
            () -> boothRepository.saveAndFlush(DemodayBooth.forProject(poll.getId(), 11, 2L)),
            "uk_demoday_booth_poll_code"
        );
    }

    @Test
    @DisplayName("서로 다른 투표에서는 같은 부스 코드를 사용할 수 있다")
    void allowSameBoothCodeAcrossPolls() {
        // Given
        DemodayPoll firstPoll = savePoll("첫 번째 부스 코드 범위");
        DemodayPoll secondPoll = savePoll("두 번째 부스 코드 범위");
        saveBooth(firstPoll, 11, 1L);

        // When & Then
        assertThatCode(() -> saveBooth(secondPoll, 11, 2L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("부스 코드는 데이터베이스에도 양수로만 저장할 수 있다")
    void rejectNonPositiveBoothCode() {
        // Given
        DemodayPoll poll = savePoll("부스 코드 양수 제약");

        // When & Then
        assertConstraintViolation(
            () -> insertBooth(poll.getId(), 0, 1L),
            "ck_demoday_booth_code_positive"
        );
    }

    @Test
    @DisplayName("같은 투표에서 한 회원의 표를 중복 저장할 수 없다")
    void rejectDuplicateMemberVoteInSamePoll() {
        // Given
        DemodayPoll poll = savePoll("회원 중복 투표");
        DemodayBooth firstBooth = saveBooth(poll, 1L);
        DemodayBooth secondBooth = saveBooth(poll, 2L);
        voteRepository.saveAndFlush(DemodayVote.forMember(poll.getId(), MEMBER_ID, firstBooth));

        // When & Then
        assertConstraintViolation(() -> voteRepository.saveAndFlush(
            DemodayVote.forMember(poll.getId(), MEMBER_ID, secondBooth)
        ), "uk_demoday_vote_poll_member");
    }

    @Test
    @DisplayName("같은 입장 코드로 표를 중복 저장할 수 없다")
    void rejectDuplicateVisitorVoteInSamePoll() {
        // Given
        DemodayPoll poll = savePoll("방문자 중복 투표");
        DemodayBooth firstBooth = saveBooth(poll, 1L);
        DemodayBooth secondBooth = saveBooth(poll, 2L);
        DemodayEntryCode entryCode = saveEntryCode(poll, "a");
        voteRepository.saveAndFlush(DemodayVote.forVisitor(poll.getId(), entryCode, firstBooth));

        // When & Then
        assertConstraintViolation(() -> voteRepository.saveAndFlush(
            DemodayVote.forVisitor(poll.getId(), entryCode, secondBooth)
        ), "uk_demoday_vote_entry_code");
    }

    @Test
    @DisplayName("표의 회원 식별자와 입장 코드 식별자가 모두 없으면 저장할 수 없다")
    void rejectVoteWithoutVoterIdentifier() {
        // Given
        DemodayPoll poll = savePoll("투표 식별자 누락");
        DemodayBooth booth = saveBooth(poll, 1L);

        // When & Then
        assertConstraintViolation(
            () -> insertVote(poll.getId(), null, null, booth.getId()),
            "ck_demoday_vote_identifier_xor"
        );
    }

    @Test
    @DisplayName("표의 회원 식별자와 입장 코드 식별자를 동시에 저장할 수 없다")
    void rejectVoteWithBothVoterIdentifiers() {
        // Given
        DemodayPoll poll = savePoll("투표 식별자 중복");
        DemodayBooth booth = saveBooth(poll, 1L);
        DemodayEntryCode entryCode = saveEntryCode(poll, "b");

        // When & Then
        assertConstraintViolation(
            () -> insertVote(poll.getId(), MEMBER_ID, entryCode.getId(), booth.getId()),
            "ck_demoday_vote_identifier_xor"
        );
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
        assertConstraintViolation(() -> voteRepository.saveAndFlush(
            DemodayVote.forMember(poll.getId(), MEMBER_ID, secondBooth)
        ), "uk_demoday_vote_poll_member");
    }

    @Test
    @DisplayName("같은 회원이 같은 부스의 스탬프를 중복 저장할 수 없다")
    void rejectDuplicateMemberStamp() {
        // Given
        DemodayPoll poll = savePoll("회원 중복 스탬프");
        DemodayBooth booth = saveBooth(poll, 1L);
        stampRepository.saveAndFlush(DemodayStamp.forMember(MEMBER_ID, booth));

        // When & Then
        assertConstraintViolation(
            () -> stampRepository.saveAndFlush(DemodayStamp.forMember(MEMBER_ID, booth)),
            "uk_demoday_stamp_member_booth"
        );
    }

    @Test
    @DisplayName("같은 입장 코드로 같은 부스의 스탬프를 중복 저장할 수 없다")
    void rejectDuplicateVisitorStamp() {
        // Given
        DemodayPoll poll = savePoll("방문자 중복 스탬프");
        DemodayBooth booth = saveBooth(poll, 1L);
        DemodayEntryCode entryCode = saveEntryCode(poll, "c");
        stampRepository.saveAndFlush(DemodayStamp.forVisitor(entryCode, booth));

        // When & Then
        assertConstraintViolation(
            () -> stampRepository.saveAndFlush(DemodayStamp.forVisitor(entryCode, booth)),
            "uk_demoday_stamp_entry_code_booth"
        );
    }

    @Test
    @DisplayName("스탬프의 회원 식별자와 입장 코드 식별자가 모두 없으면 저장할 수 없다")
    void rejectStampWithoutCollectorIdentifier() {
        // Given
        DemodayPoll poll = savePoll("스탬프 식별자 누락");
        DemodayBooth booth = saveBooth(poll, 1L);

        // When & Then
        assertConstraintViolation(
            () -> insertStamp(null, null, booth.getId()),
            "ck_demoday_stamp_identifier_xor"
        );
    }

    @Test
    @DisplayName("스탬프의 회원 식별자와 입장 코드 식별자를 동시에 저장할 수 없다")
    void rejectStampWithBothCollectorIdentifiers() {
        // Given
        DemodayPoll poll = savePoll("스탬프 식별자 중복");
        DemodayBooth booth = saveBooth(poll, 1L);
        DemodayEntryCode entryCode = saveEntryCode(poll, "d");

        // When & Then
        assertConstraintViolation(
            () -> insertStamp(MEMBER_ID, entryCode.getId(), booth.getId()),
            "ck_demoday_stamp_identifier_xor"
        );
    }

    private void assertConstraintViolation(ThrowingCallable operation, String constraintName) {
        assertThatThrownBy(operation)
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasStackTraceContaining(constraintName);
    }

    private DemodayPoll savePoll(String name) {
        return pollRepository.saveAndFlush(DemodayPoll.create(9L, name, OPENS_AT, CLOSES_AT));
    }

    private DemodayBooth saveBooth(DemodayPoll poll, Long projectId) {
        return saveBooth(poll, Math.toIntExact(projectId), projectId);
    }

    private DemodayBooth saveBooth(DemodayPoll poll, Integer boothCode, Long projectId) {
        return boothRepository.saveAndFlush(DemodayBooth.forProject(poll.getId(), boothCode, projectId));
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

    private void insertBooth(Long pollId, Integer boothCode, Long projectId) {
        jdbcTemplate.update("""
            INSERT INTO demoday_booth (
                created_at,
                updated_at,
                demoday_poll_id,
                booth_code,
                project_id
            ) VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)
            """, pollId, boothCode, projectId);
    }

    private void insertStamp(Long memberId, Long entryCodeId, Long boothId) {
        jdbcTemplate.update("""
            INSERT INTO demoday_stamp (
                created_at,
                updated_at,
                member_id,
                entry_code_id,
                booth_id
            ) VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)
            """, memberId, entryCodeId, boothId);
    }
}
