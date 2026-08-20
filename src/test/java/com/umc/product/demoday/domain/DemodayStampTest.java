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
    @DisplayName("UMC 내부 인원이 스탬프를 받으면 회원 식별자를 보관한다.")
    void createMemberStamp() {
        // given
        DemodayBooth booth = createBooth(POLL_ID);

        // when
        DemodayStamp stamp = DemodayStamp.forMember(MEMBER_ID, booth);

        // then
        assertThat(stamp.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(stamp.getEntryCodeId()).isNull();
        assertThat(stamp.getBoothId()).isEqualTo(BOOTH_ID);
        assertThat(stamp.getRevokedAt()).isNull();
        assertThat(stamp.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("UMC 외부 인원이 스탬프를 받으면 입장 코드를 보관한다.")
    void createVisitorStamp() {
        // given
        DemodayEntryCode entryCode = createEntryCode(POLL_ID);
        DemodayBooth booth = createBooth(POLL_ID);

        // when
        DemodayStamp stamp = DemodayStamp.forVisitor(entryCode, booth);

        // then
        assertThat(stamp.getMemberId()).isNull();
        assertThat(stamp.getEntryCodeId()).isEqualTo(ENTRY_CODE_ID);
        assertThat(stamp.getBoothId()).isEqualTo(BOOTH_ID);
    }

    @Test
    @DisplayName("입장 코드 날짜와 부스의 행사일이 같으면 스탬프를 생성할 수 있다.")
    void allowVisitorStampForBoothOperatingOnEntryDate() {
        // given
        DemodayEntryCode entryCode = createEntryCode(POLL_ID);
        DemodayBooth booth = createBooth(POLL_ID);

        // when & then
        assertThatCode(() -> DemodayStamp.forVisitor(entryCode, booth))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("외부인 스탬프는 입장 코드가 발급된 날과 동일한 부스에서만 받을 수 있다.")
    void rejectVisitorStampForBoothFromDifferentPoll() {
        // given
        DemodayEntryCode entryCode = createEntryCode(POLL_ID);
        DemodayBooth anotherPollBooth = createBooth(2L);

        // when & then
        assertThatThrownBy(() -> DemodayStamp.forVisitor(entryCode, anotherPollBooth))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("BaseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_STAMP_POLL_MISMATCH);
    }

    @Test
    @DisplayName("저장되지 않은 부스에서는 스탬프를 받을 수 없다.")
    void rejectStampForUnsavedBooth() {
        // given
        DemodayBooth unsavedBooth = DemodayBooth.forProject(POLL_ID, BOOTH_CODE, PROJECT_ID);

        // when & then
        assertThatThrownBy(() -> DemodayStamp.forMember(MEMBER_ID, unsavedBooth))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("무효화된 스탬프는 다시 무효화할 수 없다.")
    void rejectRevokingStampTwice() {
        // given
        DemodayStamp stamp = DemodayStamp.forMember(MEMBER_ID, createBooth(POLL_ID));
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
