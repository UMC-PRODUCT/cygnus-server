package com.umc.product.chat.application.port.in.query.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

@DisplayName("GetChatMessagesForAuthorizedCallerQuery size 불변식")
class GetChatMessagesForAuthorizedCallerQueryTest {

    @ParameterizedTest(name = "size={0} 이면 CHAT_MESSAGE_INVALID_PAGE_SIZE 예외")
    @ValueSource(ints = {0, -1, Integer.MAX_VALUE, GetChatMessagesQuery.MAX_PAGE_SIZE + 1})
    @DisplayName("size 가 1..MAX_PAGE_SIZE 범위를 벗어나면 예외를 던진다")
    void invalidSize_throws(int size) {
        assertThatThrownBy(() -> new GetChatMessagesForAuthorizedCallerQuery(1L, null, size))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_INVALID_PAGE_SIZE);
    }

    @ParameterizedTest(name = "size={0} 이면 정상 생성")
    @ValueSource(ints = {1, GetChatMessagesQuery.MAX_PAGE_SIZE})
    @DisplayName("size 가 경계값 1 / MAX_PAGE_SIZE 이면 정상 생성된다")
    void boundarySize_ok(int size) {
        assertThatCode(() -> new GetChatMessagesForAuthorizedCallerQuery(1L, null, size))
            .doesNotThrowAnyException();
        assertThat(new GetChatMessagesForAuthorizedCallerQuery(1L, null, size).size()).isEqualTo(size);
    }
}
