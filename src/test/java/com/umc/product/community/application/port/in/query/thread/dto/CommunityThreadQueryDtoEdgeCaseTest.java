package com.umc.product.community.application.port.in.query.thread.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Community thread query DTO 경계")
class CommunityThreadQueryDtoEdgeCaseTest {

    @Test
    @DisplayName("공통 query constraint는 keyword를 정규화하고 양수·unique ID를 보장한다")
    void validatesSharedQueryConstraints() {
        assertThat(ThreadQueryConstraints.requirePositive(1L, "id")).isEqualTo(1L);
        assertThat(ThreadQueryConstraints.normalizeKeyword(null)).isNull();
        assertThat(ThreadQueryConstraints.normalizeKeyword("   ")).isNull();
        assertThat(ThreadQueryConstraints.normalizeKeyword("  검색  ")).isEqualTo("검색");
        assertThat(ThreadQueryConstraints.requireUniquePositiveIds(List.of(2L, 1L), "ids"))
            .containsExactly(2L, 1L);
        ThreadQueryConstraints.requirePage(0, 100);

        assertInvalid(() -> ThreadQueryConstraints.requirePositive(0L, "id"));
        assertInvalid(() -> ThreadQueryConstraints.normalizeKeyword("가".repeat(81)));
        assertInvalid(() -> ThreadQueryConstraints.requireUniquePositiveIds(null, "ids"));
        assertInvalid(() -> ThreadQueryConstraints.requireUniquePositiveIds(List.of(1L, 1L), "ids"));
        assertInvalid(() -> ThreadQueryConstraints.requireUniquePositiveIds(Arrays.asList(1L, null), "ids"));
        assertInvalid(() -> ThreadQueryConstraints.requirePage(-1, 10));
        assertInvalid(() -> ThreadQueryConstraints.requirePage(0, 0));
        assertInvalid(() -> ThreadQueryConstraints.requirePage(0, 101));
    }

    @Test
    @DisplayName("thread 목록 query는 filter·generation·page 경계를 검증한다")
    void validatesListQueries() {
        ListThreadsQuery query = new ListThreadsQuery(1L, ThreadListFilter.ALL, " 검색 ", 0, 100);
        assertThat(query.q()).isEqualTo("검색");
        assertThat(new ListThreadMembersQuery(1L, 2L, null, null, null, null, 0, 1).q())
            .isNull();

        assertInvalid(() -> new ListThreadsQuery(1L, null, null, 0, 10));
        assertInvalid(() -> new ListThreadsQuery(1L, ThreadListFilter.ALL, null, -1, 10));
        assertInvalid(() -> new ListThreadMembersQuery(1L, 2L, null, null, null, 0L, 0, 10));
        assertInvalid(() -> new ListThreadMembersQuery(1L, 2L, null, null, null, null, 0, 101));
    }

    @Test
    @DisplayName("page DTO는 null collection을 비우고 음수 cursor·total을 거부한다")
    void validatesPageDtos() {
        assertThat(new ThreadInvitablePageInfo(null, null, 0).items()).isEmpty();
        assertThat(new ThreadMemberPageInfo(null, null, 0).items()).isEmpty();
        ThreadListInfo list = new ThreadListInfo(null, null, null, 0);
        assertThat(list.pinned()).isEmpty();
        assertThat(list.threads()).isEmpty();

        assertInvalid(() -> new ThreadInvitablePageInfo(List.of(), -1, 0));
        assertInvalid(() -> new ThreadInvitablePageInfo(List.of(), null, -1));
        assertInvalid(() -> new ThreadMemberPageInfo(List.of(), -1, 0));
        assertInvalid(() -> new ThreadMemberPageInfo(List.of(), null, -1));
        assertInvalid(() -> new ThreadListInfo(List.of(), List.of(), -1, 0));
        assertInvalid(() -> new ThreadListInfo(List.of(), List.of(), null, -1));
    }

    private void assertInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable).isInstanceOf(IllegalArgumentException.class);
    }
}
