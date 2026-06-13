package com.umc.product.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
