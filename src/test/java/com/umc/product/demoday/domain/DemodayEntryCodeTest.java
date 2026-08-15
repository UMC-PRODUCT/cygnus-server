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
}
