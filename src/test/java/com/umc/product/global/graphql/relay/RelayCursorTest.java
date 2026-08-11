package com.umc.product.global.graphql.relay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RelayCursor — Connection 오프셋 커서 인코딩/디코딩")
class RelayCursorTest {

    @Test
    void 인코딩은_패딩_없는_base64url_offset_콜론_N_형식이다() {
        // when
        String cursor = RelayCursor.encodeOffset(0L);

        // then — base64url("offset:0")
        assertThat(cursor).isEqualTo("b2Zmc2V0OjA");
        assertThat(cursor).doesNotContain("=");
    }

    @Test
    void 인코딩한_커서를_디코딩하면_offset이_복원된다() {
        // given
        String cursor = RelayCursor.encodeOffset(7L);

        // when
        long offset = RelayCursor.decodeOffset(cursor);

        // then
        assertThat(offset).isEqualTo(7L);
    }

    @Test
    void 큰_offset도_왕복_인코딩된다() {
        // given
        long largeOffset = 9_999L;

        // when
        long decoded = RelayCursor.decodeOffset(RelayCursor.encodeOffset(largeOffset));

        // then
        assertThat(decoded).isEqualTo(largeOffset);
    }

    @Test
    void null_또는_공백_커서는_예외가_발생한다() {
        assertThatThrownBy(() -> RelayCursor.decodeOffset(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be blank");
        assertThatThrownBy(() -> RelayCursor.decodeOffset("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be blank");
    }

    @Test
    void base64가_아닌_커서는_예외가_발생한다() {
        assertThatThrownBy(() -> RelayCursor.decodeOffset("!!!not-base64!!!"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a valid connection cursor");
    }

    @Test
    void offset_접두어가_없는_커서는_예외가_발생한다() {
        // given — base64url("page:3")
        String wrongPrefix = "cGFnZToz";

        // when & then
        assertThatThrownBy(() -> RelayCursor.decodeOffset(wrongPrefix))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a valid connection cursor");
    }

    @Test
    void offset이_숫자가_아닌_커서는_예외가_발생한다() {
        // given — base64url("offset:abc")
        String nonNumeric = "b2Zmc2V0OmFiYw";

        // when & then
        assertThatThrownBy(() -> RelayCursor.decodeOffset(nonNumeric))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a valid connection cursor");
    }

    @Test
    void 음수_offset_커서는_거부한다() {
        // given — base64url("offset:-1")
        String negative = "b2Zmc2V0Oi0x";

        // when & then
        assertThatThrownBy(() -> RelayCursor.decodeOffset(negative))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be negative");
    }
}
