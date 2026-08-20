package com.umc.product.demoday.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@DisplayName("DemodayVoteTest")
class DemodayVoteTest {

    private static final Long POLL_ID = 1L;
    private static final Long BOOTH_ID = 10L;
    private static final Integer BOOTH_CODE = 11;
    private static final Long ENTRY_CODE_ID = 20L;
    private static final Long MEMBER_ID = 1L;
    private static final Long PROJECT_ID = 1L;

    private static DemodayBooth createBooth(Long pollId) {
        DemodayBooth booth = DemodayBooth.forProject(pollId, BOOTH_CODE, PROJECT_ID);
        ReflectionTestUtils.setField(booth, "id", BOOTH_ID);
        return booth;
    }

    private static DemodayEntryCode createEntryCode(Long pollId) {
        DemodayEntryCode entryCode = DemodayEntryCode.create(pollId, "code hash");
        ReflectionTestUtils.setField(entryCode, "id", ENTRY_CODE_ID);
        return entryCode;
    }

    @Test
    @DisplayName("UMC 내부 인원이 투표하는 경우에는 입장 코드를 사용하지 않는다.")
    void createMemberVote() {
        //given
        DemodayBooth booth = createBooth(POLL_ID);

        //when
        DemodayVote vote = DemodayVote.forMember(POLL_ID, MEMBER_ID, booth);

        //then
        assertThat(vote.getPollId()).isEqualTo(POLL_ID);
        assertThat(vote.getTargetBoothId()).isEqualTo(BOOTH_ID);
        assertThat(vote.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(vote.getEntryCodeId()).isNull();
        assertThat(vote.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("UMC 외부 인원이 투표하는 경우에는 입장 코드를 사용해야 한다.")
    void createVisitorVote() {
        //given
        DemodayBooth booth = createBooth(POLL_ID);
        DemodayEntryCode entryCode = createEntryCode(POLL_ID);

        //when
        DemodayVote vote = DemodayVote.forVisitor(POLL_ID, entryCode, booth);

        //then
        assertThat(vote.getPollId()).isEqualTo(POLL_ID);
        assertThat(vote.getTargetBoothId()).isEqualTo(BOOTH_ID);
        assertThat(vote.getMemberId()).isNull();
        assertThat(vote.getEntryCodeId()).isEqualTo(ENTRY_CODE_ID);
        assertThat(vote.getRevokedAt()).isNull();
    }

    @Test
    @DisplayName("부스가 같은 투표에 속하면 투표할 수 있다.")
    void allowVoteForBoothInSamePoll() {
        //given
        DemodayBooth booth = createBooth(POLL_ID);

        //when & then
        assertThatCode(() -> DemodayVote.forMember(POLL_ID, MEMBER_ID, booth))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("투표와 부스의 ID가 다르면 투표할 수 없다.")
    void rejectVoteForBoothFromDifferentPoll() {
        // given
        DemodayBooth anotherPollBooth = createBooth(2L);

        // when & then
        assertThatThrownBy(() -> DemodayVote.forMember(POLL_ID, MEMBER_ID, anotherPollBooth))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("BaseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_POLL_MISMATCH);
    }

    @Test
    @DisplayName("입장 코드가 다른 투표에 속하면 투표할 수 없다.")
    void rejectVisitorVoteWithEntryCodeFromDifferentPoll() {
        // given
        DemodayBooth booth = createBooth(POLL_ID);
        DemodayEntryCode anotherPollEntryCode = createEntryCode(2L);

        // when & then
        assertThatThrownBy(() -> DemodayVote.forVisitor(POLL_ID, anotherPollEntryCode, booth))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("BaseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_POLL_MISMATCH);
    }

    @Test
    @DisplayName("저장되지 않은 부스에는 투표할 수 없다.")
    void rejectVoteForUnsavedBooth() {
        // given
        DemodayBooth unsavedBooth = DemodayBooth.forProject(POLL_ID, BOOTH_CODE, PROJECT_ID);

        // when & then
        assertThatThrownBy(() -> DemodayVote.forMember(POLL_ID, MEMBER_ID, unsavedBooth))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("회원과 외부 방문자는 외부 부스에 투표할 수 없다.")
    void rejectVoteForExternalBooth() {
        // given
        DemodayBooth externalBooth = DemodayBooth.forExternal(POLL_ID, BOOTH_CODE, "외부 부스");
        ReflectionTestUtils.setField(externalBooth, "id", BOOTH_ID);
        DemodayEntryCode entryCode = createEntryCode(POLL_ID);

        // when & then
        assertThatThrownBy(() -> DemodayVote.forMember(POLL_ID, MEMBER_ID, externalBooth))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_EXTERNAL_BOOTH_NOT_ALLOWED);

        assertThatThrownBy(() -> DemodayVote.forVisitor(POLL_ID, entryCode, externalBooth))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_EXTERNAL_BOOTH_NOT_ALLOWED);
    }

    @Test
    @DisplayName("무효화된 표는 다시 무효화 할 수 없다.")
    void rejectRevokingVoteTwice() {
        //given
        DemodayBooth booth = createBooth(POLL_ID);
        DemodayVote vote = DemodayVote.forMember(POLL_ID, MEMBER_ID, booth);

        Instant now = Instant.now();

        //when
        vote.revoke(now);

        //then
        assertThat(vote.getRevokedAt()).isEqualTo(now);
        assertThat(vote.isRevoked()).isTrue();
        assertThatThrownBy(() -> vote.revoke(now))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("BaseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_ALREADY_REVOKED);

    }

    @Test
    @DisplayName("무효화한 표를 복원하면 원래 부스와 참여자 식별자를 유지한다")
    void restoreRevokedVote() {
        // given
        DemodayVote vote = DemodayVote.forMember(POLL_ID, MEMBER_ID, createBooth(POLL_ID));
        vote.revoke(Instant.parse("2026-08-20T01:00:00Z"));

        // when
        vote.restore();

        // then
        assertThat(vote.isRevoked()).isFalse();
        assertThat(vote.getRevokedAt()).isNull();
        assertThat(vote.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(vote.getTargetBoothId()).isEqualTo(BOOTH_ID);
    }

    @Test
    @DisplayName("유효한 표는 무효 해제할 수 없다")
    void rejectRestoringValidVote() {
        // given
        DemodayVote vote = DemodayVote.forMember(POLL_ID, MEMBER_ID, createBooth(POLL_ID));

        // when & then
        assertThatThrownBy(vote::restore)
            .isInstanceOf(DemodayDomainException.class)
            .extracting("BaseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_NOT_REVOKED);
    }
}
