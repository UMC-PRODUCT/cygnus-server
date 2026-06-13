package com.umc.product.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("ChatMember.markRead")
class ChatMemberTest {

    @Test
    @DisplayName("처음 읽으면 lastReadMessageId가 설정된다")
    void markRead_first() {
        ChatMember member = ChatMember.of(1L, 10L);

        member.markRead(5L);

        assertThat(member.getLastReadMessageId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("더 최신 메시지를 읽으면 갱신된다")
    void markRead_forward() {
        ChatMember member = ChatMember.of(1L, 10L);
        member.markRead(5L);

        member.markRead(8L);

        assertThat(member.getLastReadMessageId()).isEqualTo(8L);
    }

    @Test
    @DisplayName("이미 읽은 것보다 과거이거나 같은 메시지는 무시한다")
    void markRead_backwardIgnored() {
        ChatMember member = ChatMember.of(1L, 10L);
        member.markRead(8L);

        member.markRead(5L);
        assertThat(member.getLastReadMessageId()).isEqualTo(8L);

        member.markRead(8L);
        assertThat(member.getLastReadMessageId()).isEqualTo(8L);
    }

    @Test
    @DisplayName("null이면 아무 동작도 하지 않는다")
    void markRead_null() {
        ChatMember member = ChatMember.of(1L, 10L);
        ReflectionTestUtils.setField(member, "lastReadMessageId", 3L);

        member.markRead(null);

        assertThat(member.getLastReadMessageId()).isEqualTo(3L);
    }
}
