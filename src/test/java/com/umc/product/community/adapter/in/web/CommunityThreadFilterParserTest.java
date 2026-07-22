package com.umc.product.community.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.community.application.port.in.query.thread.dto.ThreadListFilter;

@DisplayName("Community thread filter parser")
class CommunityThreadFilterParserTest {

    @Test
    @DisplayName("지원 filter wire value를 매핑하고 알 수 없는 값은 거절한다")
    void parsesSupportedValuesAndRejectsUnknownValue() {
        assertThat(CommunityThreadFilterParser.parse("all")).isEqualTo(ThreadListFilter.ALL);
        assertThat(CommunityThreadFilterParser.parse("unread")).isEqualTo(ThreadListFilter.UNREAD);
        assertThat(CommunityThreadFilterParser.parse("STUDY")).isEqualTo(ThreadListFilter.STUDY);
        assertThat(CommunityThreadFilterParser.parse("QNA")).isEqualTo(ThreadListFilter.QNA);
        assertThat(CommunityThreadFilterParser.parse("PROJECT")).isEqualTo(ThreadListFilter.PROJECT);
        assertThat(CommunityThreadFilterParser.parse("FREE")).isEqualTo(ThreadListFilter.FREE);

        assertThatThrownBy(() -> CommunityThreadFilterParser.parse("study"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported thread filter: study");
    }
}
