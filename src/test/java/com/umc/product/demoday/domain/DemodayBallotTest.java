package com.umc.product.demoday.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@DisplayName("DemodayBallotTest")
class DemodayBallotTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long PROJECT_ID = 1L;

    private static DemodayVoteEvent createVoteEvent() {
        return DemodayVoteEvent.create(
            8L,
            "8기 데모데이",
            Instant.parse("2026-08-01T05:00:00Z"),
            Instant.parse("2026-08-01T08:00:00Z")
        );
    }

    private static DemodayBooth createBooth(DemodayVoteEvent voteEvent) {
        return DemodayBooth.forProject(voteEvent, PROJECT_ID);
    }

    private static DemodayEntryCode createEntryCode(DemodayVoteEvent voteEvent) {
        return DemodayEntryCode.create(voteEvent, "code hash");
    }

    @Test
    @DisplayName("UMC 내부 인원이 투표하는 경우에는 입장 코드를 사용하지 않는다.")
    void 멤버_투표() {
        //given
        DemodayVoteEvent voteEvent = createVoteEvent();
        DemodayBooth booth = createBooth(voteEvent);

        //when
        DemodayBallot ballot = DemodayBallot.forMember(voteEvent, MEMBER_ID, booth);

        //then
        assertThat(ballot.getVoteEvent()).isSameAs(voteEvent);
        assertThat(ballot.getTargetBooth()).isSameAs(booth);
        assertThat(ballot.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(ballot.getEntryCode()).isNull();
        assertThat(ballot.isVoided()).isFalse();
    }

    @Test
    @DisplayName("UMC 외부 인원이 투표하는 경우에는 입장 코드를 사용해야 한다.")
    void 외부인_투표() {
        //given
        DemodayVoteEvent voteEvent = createVoteEvent();
        DemodayBooth booth = createBooth(voteEvent);
        DemodayEntryCode entryCode = createEntryCode(voteEvent);

        //when
        DemodayBallot ballot = DemodayBallot.forVisitor(voteEvent, entryCode, booth);

        //then
        assertThat(ballot.getVoteEvent()).isSameAs(voteEvent);
        assertThat(ballot.getTargetBooth()).isSameAs(booth);
        assertThat(ballot.getMemberId()).isNull();
        assertThat(ballot.getEntryCode()).isEqualTo(entryCode);
        assertThat(ballot.getVoidedAt()).isNull();

    }

    @Test
    @DisplayName("무효화된 표는 다시 무효화 할 수 없다.")
    void 표_무효화_검증() {
        //given
        DemodayVoteEvent voteEvent = createVoteEvent();
        DemodayBooth booth = createBooth(voteEvent);
        DemodayBallot ballot = DemodayBallot.forMember(voteEvent, MEMBER_ID, booth);

        Instant now = Instant.now();

        //when
        ballot.invalidate(now);

        //then
        assertThat(ballot.getVoidedAt()).isEqualTo(now);
        assertThat(ballot.isVoided()).isTrue();
        assertThatThrownBy(() -> ballot.invalidate(now))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("BaseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_BALLOT_ALREADY_VOIDED);

    }
}
