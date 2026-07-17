package com.umc.product.chat.application.port.in.query.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOwnerReference;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

@DisplayName("GetChatMessagesQuery size 불변식")
class GetChatMessagesQueryTest {

    private static final ChatRoomOwnerReference OWNER = ChatRoomOwnerReference.standalone(1L);
    private static final ChatRoomActorContext ACTOR = ChatRoomActorContext.actor(10L);

    @ParameterizedTest(name = "size={0} 이면 CHAT_MESSAGE_INVALID_PAGE_SIZE 예외")
    @ValueSource(ints = {0, -1, Integer.MAX_VALUE, GetChatMessagesQuery.MAX_PAGE_SIZE + 1})
    @DisplayName("size 가 1..MAX_PAGE_SIZE 범위를 벗어나면 예외를 던진다")
    void invalidSize_throws(int size) {
        assertThatThrownBy(() -> new GetChatMessagesQuery(OWNER, ACTOR, null, size))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_INVALID_PAGE_SIZE);
    }

    @ParameterizedTest(name = "size={0} 이면 정상 생성")
    @ValueSource(ints = {1, GetChatMessagesQuery.MAX_PAGE_SIZE})
    @DisplayName("size 가 경계값 1 / MAX_PAGE_SIZE 이면 정상 생성된다")
    void boundarySize_ok(int size) {
        assertThatCode(() -> new GetChatMessagesQuery(OWNER, ACTOR, null, size))
            .doesNotThrowAnyException();
        assertThat(new GetChatMessagesQuery(OWNER, ACTOR, null, size).size()).isEqualTo(size);
    }
}
