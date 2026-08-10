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

@DisplayName("DemodayStampTest")
class DemodayStampTest {

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

    @Test
    @DisplayName("UMC 내부 인원이 스탬프를 받으면 회원 식별자를 보관한다.")
    void 멤버_스탬프_생성() {
        // given
        DemodayBooth booth = createBooth(createPoll());

        // when
        DemodayStamp stamp = DemodayStamp.forMember(MEMBER_ID, booth);

        // then
        assertThat(stamp.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(stamp.getEntryCode()).isNull();
        assertThat(stamp.getBooth()).isSameAs(booth);
        assertThat(stamp.getRevokedAt()).isNull();
        assertThat(stamp.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("UMC 외부 인원이 스탬프를 받으면 입장 코드를 보관한다.")
    void 외부인_스탬프_생성() {
        // given
        DemodayPoll poll = createPoll();
        DemodayEntryCode entryCode = DemodayEntryCode.create(poll, "code hash");
        DemodayBooth booth = createBooth(poll);

        // when
        DemodayStamp stamp = DemodayStamp.forVisitor(entryCode, booth);

        // then
        assertThat(stamp.getMemberId()).isNull();
        assertThat(stamp.getEntryCode()).isSameAs(entryCode);
        assertThat(stamp.getBooth()).isSameAs(booth);
    }

    @Test
    @DisplayName("입장 코드와 부스의 투표 ID가 같으면 스탬프를 생성할 수 있다.")
    void 입장_코드와_부스의_투표_ID가_같으면_스탬프를_생성한다() {
        // given
        DemodayPoll pollLoadedForEntryCode = createPersistedPoll(1L);
        DemodayEntryCode entryCode = DemodayEntryCode.create(pollLoadedForEntryCode, "code hash");
        DemodayPoll pollLoadedForBooth = createPersistedPoll(1L);
        DemodayBooth booth = createBooth(pollLoadedForBooth);

        // when & then
        assertThat(pollLoadedForEntryCode).isNotSameAs(pollLoadedForBooth);
        assertThat(pollLoadedForEntryCode.getId())
            .isNotNull()
            .isEqualTo(pollLoadedForBooth.getId());
        assertThatCode(() -> DemodayStamp.forVisitor(entryCode, booth))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("외부인 스탬프는 입장 코드와 같은 투표의 부스에서만 받을 수 있다.")
    void 외부인_스탬프_투표_불일치_검증() {
        // given
        DemodayEntryCode entryCode = DemodayEntryCode.create(createPoll(), "code hash");
        DemodayBooth anotherPollBooth = createBooth(createPoll());

        // when & then
        assertThatThrownBy(() -> DemodayStamp.forVisitor(entryCode, anotherPollBooth))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("BaseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_STAMP_POLL_MISMATCH);
    }

    @Test
    @DisplayName("무효화된 스탬프는 다시 무효화할 수 없다.")
    void 스탬프_무효화_검증() {
        // given
        DemodayStamp stamp = DemodayStamp.forMember(MEMBER_ID, createBooth(createPoll()));
        Instant revokedAt = Instant.parse("2026-08-01T06:00:00Z");

        // when
        stamp.revoke(revokedAt);

        // then
        assertThat(stamp.getRevokedAt()).isEqualTo(revokedAt);
        assertThat(stamp.isRevoked()).isTrue();
        assertThatThrownBy(() -> stamp.revoke(revokedAt))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("BaseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_STAMP_ALREADY_REVOKED);
    }
}
