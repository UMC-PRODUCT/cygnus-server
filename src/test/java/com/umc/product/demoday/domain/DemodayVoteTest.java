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

    private static final Long MEMBER_ID = 1L;
    private static final Long PROJECT_ID = 1L;

    private static DemodayPoll createPoll() {
        return DemodayPoll.create(
            8L,
            "8기 데모데이",
            Instant.parse("2026-08-01T05:00:00Z"),
            Instant.parse("2026-08-01T08:00:00Z")
        );
    }

    private static DemodayPoll createPersistedPoll(Long id) {
        DemodayPoll poll = createPoll();
        ReflectionTestUtils.setField(poll, "id", id);
        return poll;
    }

    private static DemodayBooth createBooth(DemodayPoll poll) {
        return DemodayBooth.forProject(poll, PROJECT_ID);
    }

    private static DemodayEntryCode createEntryCode(DemodayPoll poll) {
        return DemodayEntryCode.create(poll, "code hash");
    }

    @Test
    @DisplayName("UMC 내부 인원이 투표하는 경우에는 입장 코드를 사용하지 않는다.")
    void 멤버_투표() {
        //given
        DemodayPoll poll = createPoll();
        DemodayBooth booth = createBooth(poll);

        //when
        DemodayVote vote = DemodayVote.forMember(poll, MEMBER_ID, booth);

        //then
        assertThat(vote.getPoll()).isSameAs(poll);
        assertThat(vote.getTargetBooth()).isSameAs(booth);
        assertThat(vote.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(vote.getEntryCode()).isNull();
        assertThat(vote.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("UMC 외부 인원이 투표하는 경우에는 입장 코드를 사용해야 한다.")
    void 외부인_투표() {
        //given
        DemodayPoll poll = createPoll();
        DemodayBooth booth = createBooth(poll);
        DemodayEntryCode entryCode = createEntryCode(poll);

        //when
        DemodayVote vote = DemodayVote.forVisitor(poll, entryCode, booth);

        //then
        assertThat(vote.getPoll()).isSameAs(poll);
        assertThat(vote.getTargetBooth()).isSameAs(booth);
        assertThat(vote.getMemberId()).isNull();
        assertThat(vote.getEntryCode()).isEqualTo(entryCode);
        assertThat(vote.getRevokedAt()).isNull();

    }

    @Test
    @DisplayName("서로 다른 인스턴스라도 ID가 같으면 같은 투표로 판단한다.")
    void 다른_인스턴스여도_ID가_같으면_같은_투표로_판단() {
        //given
        DemodayPoll pollLoadedForBooth = createPersistedPoll(1L);
        DemodayBooth booth = createBooth(pollLoadedForBooth);
        DemodayPoll pollLoadedForVote = createPersistedPoll(1L);

        //when & then
        assertThat(pollLoadedForVote).isNotSameAs(pollLoadedForBooth);
        assertThat(pollLoadedForBooth.getId())
            .isNotNull()
            .isEqualTo(pollLoadedForVote.getId());

        assertThatCode(() -> DemodayVote.forMember(pollLoadedForVote, MEMBER_ID, booth))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("투표와 부스의 ID가 다르면 투표할 수 없다.")
    void 투표와_부스의_ID가_다르면_예외가_발생한다() {
        // given
        DemodayPoll poll = createPersistedPoll(1L);
        DemodayBooth booth = createBooth(createPersistedPoll(2L));

        // when & then
        assertThatThrownBy(() -> DemodayVote.forMember(poll, MEMBER_ID, booth))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("BaseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_POLL_MISMATCH);
    }

    @Test
    @DisplayName("무효화된 표는 다시 무효화 할 수 없다.")
    void 표_무효화_검증() {
        //given
        DemodayPoll poll = createPoll();
        DemodayBooth booth = createBooth(poll);
        DemodayVote vote = DemodayVote.forMember(poll, MEMBER_ID, booth);

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
