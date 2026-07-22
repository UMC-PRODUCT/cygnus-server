package com.umc.product.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.application.port.in.command.dto.CreateChatRoomCommand;
import com.umc.product.chat.application.port.in.command.dto.LeaveChatRoomCommand;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

@DisplayName("chat value와 event 잔여 계약")
class ChatValueResidualTest {

    @Test
    @DisplayName("command factory는 입력 ID를 변경하지 않는다")
    void command_factories() {
        assertThat(CreateChatRoomCommand.from(10L).creatorMemberId()).isEqualTo(10L);
        assertThat(LeaveChatRoomCommand.of(1L, 10L))
            .isEqualTo(new LeaveChatRoomCommand(1L, 10L));
    }

    @Test
    @DisplayName("7개 필드 ChatMessageInfo 생성자는 reply ID를 null로 둔다")
    void chat_message_info_legacy_constructor() {
        Instant createdAt = Instant.parse("2026-07-22T00:00:00Z");

        ChatMessageInfo info = new ChatMessageInfo(
            100L,
            1L,
            10L,
            MessageContentType.TEXT,
            "message",
            List.of(),
            createdAt
        );

        assertThat(info.replyToMessageId()).isNull();
    }

    @Test
    @DisplayName("메시지 생성 event는 전체 payload와 고정 event type을 보존한다")
    void chat_message_created_event() {
        Instant createdAt = Instant.parse("2026-07-22T00:00:00Z");
        ChatMessage message = ChatMessage.create(
            1L,
            10L,
            MessageContentType.FILE,
            "caption",
            List.of("file-1"),
            90L
        );
        ReflectionTestUtils.setField(message, "id", 100L);
        ReflectionTestUtils.setField(message, "createdAt", createdAt);

        ChatMessageCreatedEvent event = ChatMessageCreatedEvent.from(message);
        ChatMessageCreatedEvent legacy = new ChatMessageCreatedEvent(
            UUID.randomUUID(),
            createdAt,
            101L,
            1L,
            10L,
            MessageContentType.TEXT,
            "legacy",
            List.of()
        );

        assertThat(event.messageId()).isEqualTo(100L);
        assertThat(event.replyToMessageId()).isEqualTo(90L);
        assertThat(event.fileMetadataIds()).containsExactly("file-1");
        assertThat(event.eventType()).isEqualTo("chat.message.created");
        assertThat(legacy.replyToMessageId()).isNull();
    }

    @Test
    @DisplayName("ChatDomainException은 기본·사용자 message 생성자를 모두 지원한다")
    void chat_domain_exception_constructors() {
        ChatDomainException basic = new ChatDomainException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
        ChatDomainException custom = new ChatDomainException(
            ChatErrorCode.CHAT_ROOM_NOT_FOUND,
            "custom"
        );

        assertThat(basic.getBaseCode()).isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
        assertThat(custom.getMessage()).isEqualTo("custom");
    }
}
