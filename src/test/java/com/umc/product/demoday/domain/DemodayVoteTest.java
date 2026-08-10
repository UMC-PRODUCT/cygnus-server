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
    private static final Long ENTRY_CODE_ID = 20L;
    private static final Long MEMBER_ID = 1L;
    private static final Long PROJECT_ID = 1L;

    private static DemodayBooth createBooth(Long pollId) {
        DemodayBooth booth = DemodayBooth.forProject(pollId, PROJECT_ID);
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
    void 멤버_투표() {
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
    void 외부인_투표() {
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
    void 같은_투표의_부스에는_투표할_수_있다() {
        //given
        DemodayBooth booth = createBooth(POLL_ID);

        //when & then
        assertThatCode(() -> DemodayVote.forMember(POLL_ID, MEMBER_ID, booth))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("투표와 부스의 ID가 다르면 투표할 수 없다.")
    void 투표와_부스의_ID가_다르면_예외가_발생한다() {
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
    void 입장_코드의_투표가_다르면_예외가_발생한다() {
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
    void 저장되지_않은_부스에는_투표할_수_없다() {
        // given
        DemodayBooth unsavedBooth = DemodayBooth.forProject(POLL_ID, PROJECT_ID);

        // when & then
        assertThatThrownBy(() -> DemodayVote.forMember(POLL_ID, MEMBER_ID, unsavedBooth))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("무효화된 표는 다시 무효화 할 수 없다.")
    void 표_무효화_검증() {
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
}
