package com.umc.product.demoday.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@DisplayName("DemodayEntryCodeTest")
class DemodayEntryCodeTest {

    private static final Long POLL_ID = 1L;
    private static final String CODE_HASH = "code hash";
    private static final String REQUEST_ID_HASH = "request id hash";

    @Test
    @DisplayName("입장 코드를 생성하면 투표와 코드 해시를 보관한다.")
    void createEntryCode() {
        // when
        DemodayEntryCode entryCode = DemodayEntryCode.create(POLL_ID, CODE_HASH);

        // then
        assertThat(entryCode.getPollId()).isEqualTo(POLL_ID);
        assertThat(entryCode.getCodeHash()).isEqualTo(CODE_HASH);
        assertThat(entryCode.getBoundIdentityHash()).isNull();
        assertThat(entryCode.getRedeemedAt()).isNull();
        assertThat(entryCode.getRedemptionRequestIdHash()).isNull();
        assertThat(entryCode.isRedeemed()).isFalse();
    }

    @Test
    @DisplayName("사용하지 않은 입장 코드는 한 번만 사용할 수 있다.")
    void useEntryCode() {
        // given
        DemodayEntryCode entryCode = DemodayEntryCode.create(POLL_ID, CODE_HASH);
        Instant redeemedAt = Instant.parse("2026-08-01T05:30:00Z");

        // when
        entryCode.redeem(redeemedAt);

        // then
        assertThat(entryCode.getRedeemedAt()).isEqualTo(redeemedAt);
        assertThat(entryCode.isRedeemed()).isTrue();
        assertThatThrownBy(() -> entryCode.redeem(redeemedAt))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("BaseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_ENTRY_CODE_ALREADY_REDEEMED);
    }

    @Test
    @DisplayName("동일한 요청 식별자로 사용을 재개하면 최초 사용 시각을 유지한다.")
    void resumeEntryCodeWithSameRequestId() {
        // given
        DemodayEntryCode entryCode = DemodayEntryCode.create(POLL_ID, CODE_HASH);
        Instant redeemedAt = Instant.parse("2026-08-01T05:30:00Z");
        Instant retriedAt = redeemedAt.plusSeconds(10);

        // when
        boolean firstRedemption = entryCode.redeemOrResume(redeemedAt, REQUEST_ID_HASH);
        boolean resumed = entryCode.redeemOrResume(retriedAt, REQUEST_ID_HASH);

        // then
        assertThat(firstRedemption).isTrue();
        assertThat(resumed).isFalse();
        assertThat(entryCode.getRedeemedAt()).isEqualTo(redeemedAt);
        assertThat(entryCode.getRedemptionRequestIdHash()).isEqualTo(REQUEST_ID_HASH);
    }

    @Test
    @DisplayName("이미 사용된 입장 코드를 다른 요청 식별자로 재개할 수 없다.")
    void rejectDifferentRequestId() {
        // given
        DemodayEntryCode entryCode = DemodayEntryCode.create(POLL_ID, CODE_HASH);
        Instant redeemedAt = Instant.parse("2026-08-01T05:30:00Z");
        entryCode.redeemOrResume(redeemedAt, REQUEST_ID_HASH);

        // when & then
        assertThatThrownBy(() -> entryCode.redeemOrResume(redeemedAt, "another request id hash"))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("BaseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_ENTRY_CODE_ALREADY_REDEEMED);
    }

    @Test
    @DisplayName("식별자 없이 사용된 기존 입장 코드는 새 요청 식별자에 귀속되지 않는다.")
    void rejectRequestIdForLegacyRedemption() {
        // given
        DemodayEntryCode entryCode = DemodayEntryCode.create(POLL_ID, CODE_HASH);
        Instant redeemedAt = Instant.parse("2026-08-01T05:30:00Z");
        entryCode.redeem(redeemedAt);

        // when & then
        assertThatThrownBy(() -> entryCode.redeemOrResume(redeemedAt, REQUEST_ID_HASH))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("BaseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_ENTRY_CODE_ALREADY_REDEEMED);
    }
}
