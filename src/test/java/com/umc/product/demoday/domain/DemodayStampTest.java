package com.umc.product.demoday.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@DisplayName("DemodayStampTest")
class DemodayStampTest {

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

    @Test
    @DisplayName("UMC 내부 인원이 스탬프를 받으면 회원 식별자를 보관한다.")
    void 멤버_스탬프_생성() {
        // given
        DemodayBooth booth = createBooth(createVoteEvent());

        // when
        DemodayStamp stamp = DemodayStamp.forMember(MEMBER_ID, booth);

        // then
        assertThat(stamp.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(stamp.getEntryCode()).isNull();
        assertThat(stamp.getBooth()).isSameAs(booth);
        assertThat(stamp.getVoidedAt()).isNull();
        assertThat(stamp.isVoided()).isFalse();
    }

    @Test
    @DisplayName("UMC 외부 인원이 스탬프를 받으면 입장 코드를 보관한다.")
    void 외부인_스탬프_생성() {
        // given
        DemodayVoteEvent voteEvent = createVoteEvent();
        DemodayEntryCode entryCode = DemodayEntryCode.create(voteEvent, "code hash");
        DemodayBooth booth = createBooth(voteEvent);

        // when
        DemodayStamp stamp = DemodayStamp.forVisitor(entryCode, booth);

        // then
        assertThat(stamp.getMemberId()).isNull();
        assertThat(stamp.getEntryCode()).isSameAs(entryCode);
        assertThat(stamp.getBooth()).isSameAs(booth);
    }

    @Test
    @DisplayName("외부인 스탬프는 입장 코드와 같은 투표 이벤트의 부스에서만 받을 수 있다.")
    void 외부인_스탬프_투표_이벤트_불일치_검증() {
        // given
        DemodayEntryCode entryCode = DemodayEntryCode.create(createVoteEvent(), "code hash");
        DemodayBooth anotherEventBooth = createBooth(createVoteEvent());

        // when & then
        assertThatThrownBy(() -> DemodayStamp.forVisitor(entryCode, anotherEventBooth))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("BaseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_STAMP_VOTE_EVENT_MISMATCH);
    }

    @Test
    @DisplayName("무효화된 스탬프는 다시 무효화할 수 없다.")
    void 스탬프_무효화_검증() {
        // given
        DemodayStamp stamp = DemodayStamp.forMember(MEMBER_ID, createBooth(createVoteEvent()));
        Instant voidedAt = Instant.parse("2026-08-01T06:00:00Z");

        // when
        stamp.invalidate(voidedAt);

        // then
        assertThat(stamp.getVoidedAt()).isEqualTo(voidedAt);
        assertThat(stamp.isVoided()).isTrue();
        assertThatThrownBy(() -> stamp.invalidate(voidedAt))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("BaseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_STAMP_ALREADY_VOIDED);
    }
}
