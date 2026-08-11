package com.umc.product.global.graphql.relay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GlobalId — Relay 전역 ID 인코딩/디코딩")
class GlobalIdTest {

    @Test
    void 인코딩은_패딩_없는_base64url_타입명_콜론_rawId_형식이다() {
        // when
        String encoded = GlobalId.encode(GlobalIdTypes.MEMBER, 42L);

        // then — base64url("Member:42")
        assertThat(encoded).isEqualTo("TWVtYmVyOjQy");
        assertThat(encoded).doesNotContain("=");
    }

    @Test
    void 인코딩한_전역_ID를_디코딩하면_타입명과_rawId가_복원된다() {
        // given
        String encoded = GlobalId.encode(GlobalIdTypes.GISU, 7L);

        // when
        GlobalId decoded = GlobalId.decode(encoded);

        // then
        assertThat(decoded.typeName()).isEqualTo(GlobalIdTypes.GISU);
        assertThat(decoded.rawId()).isEqualTo("7");
        assertThat(decoded.rawIdAsLong()).isEqualTo(7L);
    }

    @Test
    void decodeLong은_기대_타입과_일치하면_raw_Long을_반환한다() {
        // given
        String encoded = GlobalId.encode(GlobalIdTypes.PROJECT, 123L);

        // when
        long rawId = GlobalId.decodeLong(encoded, GlobalIdTypes.PROJECT);

        // then
        assertThat(rawId).isEqualTo(123L);
    }

    @Test
    void 문자열_rawId도_왕복_인코딩된다() {
        // given
        String encoded = GlobalId.encode(GlobalIdTypes.FILE, "abc-def");

        // when
        GlobalId decoded = GlobalId.decode(encoded);

        // then
        assertThat(decoded.typeName()).isEqualTo(GlobalIdTypes.FILE);
        assertThat(decoded.rawId()).isEqualTo("abc-def");
    }

    @Test
    void rawId에_콜론이_포함되면_첫_콜론_기준으로_분리한다() {
        // given
        String encoded = GlobalId.encode(GlobalIdTypes.FILE, "a:b");

        // when
        GlobalId decoded = GlobalId.decode(encoded);

        // then
        assertThat(decoded.typeName()).isEqualTo(GlobalIdTypes.FILE);
        assertThat(decoded.rawId()).isEqualTo("a:b");
    }

    @Test
    void 기대_타입과_다른_타입의_전역_ID면_예외가_발생한다() {
        // given — Member 전역 ID를 Gisu 자리에 넘기는 실수
        String memberId = GlobalId.encode(GlobalIdTypes.MEMBER, 1L);

        // when & then
        assertThatThrownBy(() -> GlobalId.decodeLong(memberId, GlobalIdTypes.GISU))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(GlobalIdTypes.GISU)
            .hasMessageContaining(GlobalIdTypes.MEMBER);
    }

    @Test
    void base64가_아닌_값은_예외가_발생한다() {
        assertThatThrownBy(() -> GlobalId.decode("!!!not-base64!!!"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a valid global id");
    }

    @Test
    void 구분자가_없는_값은_예외가_발생한다() {
        // given — base64url("Member42")
        String noSeparator = "TWVtYmVyNDI";

        // when & then
        assertThatThrownBy(() -> GlobalId.decode(noSeparator))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a valid global id");
    }

    @Test
    void 타입명이_비어있는_값은_예외가_발생한다() {
        // given — base64url(":42")
        String emptyTypeName = "OjQy";

        // when & then
        assertThatThrownBy(() -> GlobalId.decode(emptyTypeName))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a valid global id");
    }

    @Test
    void rawId가_비어있는_값은_예외가_발생한다() {
        // given — base64url("Member:")
        String emptyRawId = "TWVtYmVyOg";

        // when & then
        assertThatThrownBy(() -> GlobalId.decode(emptyRawId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a valid global id");
    }

    @Test
    void null_또는_공백_값은_예외가_발생한다() {
        assertThatThrownBy(() -> GlobalId.decode(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be blank");
        assertThatThrownBy(() -> GlobalId.decode("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be blank");
    }

    @Test
    void rawId가_숫자가_아니면_decodeLong에서_예외가_발생한다() {
        // given — base64url("Member:abc")
        String nonNumeric = GlobalId.encode(GlobalIdTypes.MEMBER, "abc");

        // when & then
        assertThatThrownBy(() -> GlobalId.decodeLong(nonNumeric, GlobalIdTypes.MEMBER))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("numeric");
    }

    @Test
    void decodeLongs는_중복을_제거하고_첫_등장_순서를_보존한다() {
        // given
        List<String> ids = List.of(
            GlobalId.encode(GlobalIdTypes.MEMBER, 3L),
            GlobalId.encode(GlobalIdTypes.MEMBER, 1L),
            GlobalId.encode(GlobalIdTypes.MEMBER, 3L),
            GlobalId.encode(GlobalIdTypes.MEMBER, 2L),
            GlobalId.encode(GlobalIdTypes.MEMBER, 1L)
        );

        // when
        List<Long> rawIds = GlobalId.decodeLongs(ids, GlobalIdTypes.MEMBER);

        // then
        assertThat(rawIds).containsExactly(3L, 1L, 2L);
    }

    @Test
    void decodeLongs에_다른_타입이_섞이면_예외가_발생한다() {
        // given
        List<String> ids = List.of(
            GlobalId.encode(GlobalIdTypes.MEMBER, 1L),
            GlobalId.encode(GlobalIdTypes.GISU, 2L)
        );

        // when & then
        assertThatThrownBy(() -> GlobalId.decodeLongs(ids, GlobalIdTypes.MEMBER))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(GlobalIdTypes.MEMBER);
    }
}
