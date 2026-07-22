package com.umc.product.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

@DisplayName("ChatMessage")
class ChatMessageTest {

    private static final String CLIENT_PAYLOAD_FINGERPRINT = "a".repeat(64);

    @Test
    @DisplayName("일반 메시지를 생성한다")
    void create() {
        ChatMessage message = ChatMessage.create(1L, 10L, MessageContentType.TEXT, "안녕하세요", List.of("file-1"));

        assertThat(message.getRoomId()).isEqualTo(1L);
        assertThat(message.getSenderMemberId()).isEqualTo(10L);
        assertThat(message.getContentType()).isEqualTo(MessageContentType.TEXT);
        assertThat(message.getContent()).isEqualTo("안녕하세요");
        assertThat(message.getFileMetadataIds()).containsExactly("file-1");
        assertThat(message.getReplyToMessageId()).isNull();
        assertThat(message.getClientMessageId()).isNull();
        assertThat(message.getClientPayloadFingerprint()).isNull();
    }

    @Test
    @DisplayName("답장 대상 메시지 id를 가진 일반 메시지를 생성한다")
    void create_withReplyToMessageId() {
        ChatMessage message = ChatMessage.create(
            1L,
            10L,
            MessageContentType.IMAGE,
            "이미지 답장",
            List.of("file-1"),
            100L
        );

        assertThat(message.getReplyToMessageId()).isEqualTo(100L);
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
        assertThat(message.getReplyToMessageId()).isNull();
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
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_ATTACHMENT_REQUIRED);
    }

    @Test
    @DisplayName("FILE 메시지에 첨부가 없으면 생성할 수 없다")
    void create_fileWithoutFiles() {
        assertThatThrownBy(() -> ChatMessage.create(1L, 10L, MessageContentType.FILE, null, null))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_ATTACHMENT_REQUIRED);
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

    @Test
    @DisplayName("canonical payload 비교는 content와 파일 순서와 reply를 정확히 비교한다")
    void hasCanonicalPayload_exact() {
        ChatMessage message = ChatMessage.create(
            1L,
            10L,
            MessageContentType.IMAGE,
            "캡션",
            List.of("file-b", "file-a"),
            90L,
            UUID.randomUUID(),
            CLIENT_PAYLOAD_FINGERPRINT
        );

        assertThat(message.hasCanonicalPayload(
            MessageContentType.IMAGE,
            "캡션",
            List.of("file-b", "file-a"),
            90L
        )).isTrue();
        assertThat(message.hasCanonicalPayload(
            MessageContentType.IMAGE,
            "캡션",
            List.of("file-a", "file-b"),
            90L
        )).isFalse();
    }

    @Test
    @DisplayName("clientMessageId만 있고 fingerprint가 없는 메시지는 생성할 수 없다")
    void create_clientMessageIdWithoutFingerprintRejected() {
        assertThatThrownBy(() -> ChatMessage.create(
            1L,
            10L,
            MessageContentType.TEXT,
            "본문",
            List.of(),
            null,
            UUID.fromString("bb18eb33-f0f8-4a44-bd48-0f8795d696eb"),
            null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("같은 내용 edit은 editedAt을 바꾸지 않는다")
    void edit_sameContentNoop() {
        ChatMessage message = ChatMessage.create(1L, 10L, MessageContentType.TEXT, "본문", List.of());

        boolean changed = message.editContent("본문");

        assertThat(changed).isFalse();
        assertThat(message.getEditedAt()).isNull();
    }

    @Test
    @DisplayName("tombstone은 identity, sender, reply, createdAt을 보존하고 SYSTEM payload로 바꾼다")
    void tombstone_preservesIdentityAndReply() {
        ChatMessage message = ChatMessage.create(
            1L,
            10L,
            MessageContentType.IMAGE,
            "캡션",
            List.of("file-1"),
            90L,
            UUID.randomUUID(),
            CLIENT_PAYLOAD_FINGERPRINT
        );
        ReflectionTestUtils.setField(message, "id", 100L);
        ReflectionTestUtils.setField(message, "createdAt", java.time.Instant.parse("2026-07-18T00:00:00Z"));

        boolean changed = message.tombstone();

        assertThat(changed).isTrue();
        assertThat(message.getId()).isEqualTo(100L);
        assertThat(message.getSenderMemberId()).isEqualTo(10L);
        assertThat(message.getReplyToMessageId()).isEqualTo(90L);
        assertThat(message.getCreatedAt()).isEqualTo(java.time.Instant.parse("2026-07-18T00:00:00Z"));
        assertThat(message.getContentType()).isEqualTo(MessageContentType.SYSTEM);
        assertThat(message.getContent()).isEqualTo(ChatMessage.DELETED_CONTENT);
        assertThat(message.getFileMetadataIds()).isEmpty();
        assertThat(message.getDeletedAt()).isNotNull();
    }
}
