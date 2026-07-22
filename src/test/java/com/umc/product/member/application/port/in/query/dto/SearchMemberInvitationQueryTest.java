package com.umc.product.member.application.port.in.query.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SearchMemberInvitationQuery")
class SearchMemberInvitationQueryTest {

    @Test
    @DisplayName("검색어 공백과 제외 목록을 정규화한다")
    void 검색어_공백과_제외_목록을_정규화한다() {
        // when
        SearchMemberInvitationQuery query = new SearchMemberInvitationQuery(
            "  Alice  ",
            Set.of(10L),
            2,
            20
        );

        // then
        assertThat(query.keyword()).isEqualTo("Alice");
        assertThat(query.excludedMemberIds()).containsExactly(10L);
    }

    @Test
    @DisplayName("페이지 입력 범위를 벗어나면 거부한다")
    void 페이지_입력_범위를_벗어나면_거부한다() {
        // when / then
        assertThatThrownBy(() -> new SearchMemberInvitationQuery(null, Set.of(), -1, 20))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SearchMemberInvitationQuery(null, Set.of(), 0, 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SearchMemberInvitationQuery(null, Set.of(), 0, 101))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("검색어가 80 코드 포인트를 넘으면 거부한다")
    void 검색어_길이_상한을_검증한다() {
        // given
        String keyword = "가".repeat(81);

        // when / then
        assertThatThrownBy(() -> new SearchMemberInvitationQuery(keyword, Set.of(), 0, 20))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
