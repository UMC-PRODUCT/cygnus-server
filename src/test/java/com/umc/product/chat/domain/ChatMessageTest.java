package com.umc.product.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

@DisplayName("ChatMessage")
class ChatMessageTest {

    @Test
    @DisplayName("일반 메시지를 생성한다")
    void create() {
        ChatMessage message = ChatMessage.create(1L, 10L, MessageContentType.TEXT, "안녕하세요", List.of("file-1"));

        assertThat(message.getRoomId()).isEqualTo(1L);
        assertThat(message.getSenderMemberId()).isEqualTo(10L);
        assertThat(message.getContentType()).isEqualTo(MessageContentType.TEXT);
        assertThat(message.getContent()).isEqualTo("안녕하세요");
        assertThat(message.getFileMetadataIds()).containsExactly("file-1");
    }

    @Test
    @DisplayName("fileMetadataIds가 null이면 빈 리스트로 초기화된다")
    void create_withNullFiles() {
        ChatMessage message = ChatMessage.create(1L, 10L, MessageContentType.TEXT, "hi", null);

        assertThat(message.getFileMetadataIds()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("시스템 메시지는 발신자가 없고 SYSTEM 타입이다")
    void createSystem() {
        ChatMessage message = ChatMessage.createSystem(1L, "님이 입장했습니다");

        assertThat(message.getSenderMemberId()).isNull();
        assertThat(message.getContentType()).isEqualTo(MessageContentType.SYSTEM);
        assertThat(message.getContent()).isEqualTo("님이 입장했습니다");
        assertThat(message.getFileMetadataIds()).isEmpty();
    }

    @Test
    @DisplayName("TEXT 메시지에 본문이 없으면 생성할 수 없다")
    void create_textWithoutContent() {
        assertThatThrownBy(() -> ChatMessage.create(1L, 10L, MessageContentType.TEXT, "   ", List.of()))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_EMPTY);
    }

    @Test
    @DisplayName("IMAGE 메시지에 첨부가 없으면 생성할 수 없다")
    void create_imageWithoutFiles() {
        assertThatThrownBy(() -> ChatMessage.create(1L, 10L, MessageContentType.IMAGE, "캡션", List.of()))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_MISSING_ATTACHMENT);
    }

    @Test
    @DisplayName("FILE 메시지에 첨부가 없으면 생성할 수 없다")
    void create_fileWithoutFiles() {
        assertThatThrownBy(() -> ChatMessage.create(1L, 10L, MessageContentType.FILE, null, null))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_MISSING_ATTACHMENT);
    }

    @Test
    @DisplayName("IMAGE 메시지는 첨부만 있으면 캡션(content) 없이도 생성된다")
    void create_imageWithoutCaption() {
        assertThatCode(() -> ChatMessage.create(1L, 10L, MessageContentType.IMAGE, null, List.of("file-1")))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SYSTEM 타입은 create 팩토리로 생성할 수 없다")
    void create_systemRejected() {
        assertThatThrownBy(() -> ChatMessage.create(1L, 10L, MessageContentType.SYSTEM, "x", List.of()))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_INVALID_CONTENT_TYPE);
    }
}
