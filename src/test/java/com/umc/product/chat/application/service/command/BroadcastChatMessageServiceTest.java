package com.umc.product.chat.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.chat.application.port.in.command.dto.BroadcastChatMessagePayload;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;
import com.umc.product.global.websocket.application.port.out.BroadcastPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("BroadcastChatMessageService")
class BroadcastChatMessageServiceTest {

    @Mock
    BroadcastPort broadcastPort;

    @InjectMocks
    BroadcastChatMessageService sut;

    @Test
    @DisplayName("채팅 메시지 생성 이벤트를 수신하여 올바른 destination 경로로 브로드캐스트한다")
    void broadcast_올바른_destination_경로_조립() {
        // given
        ChatMessageCreatedEvent event = new ChatMessageCreatedEvent(
            UUID.randomUUID(),
            Instant.parse("2026-07-03T07:00:00Z"),
            100L,
            10L,
            5L,
            MessageContentType.TEXT,
            "안녕하세요",
            List.of()
        );

        // when
        sut.broadcast(event);

        // then
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<BroadcastChatMessagePayload> payloadCaptor = ArgumentCaptor.forClass(BroadcastChatMessagePayload.class);
        then(broadcastPort).should().broadcast(destinationCaptor.capture(), payloadCaptor.capture());

        // destination 검증: /topic/chat/rooms/{roomId}/messages
        assertThat(destinationCaptor.getValue()).isEqualTo("/topic/chat/rooms/10/messages");

        // payload 검증: 이벤트 필드가 정확히 변환되었는지 확인
        BroadcastChatMessagePayload payload = payloadCaptor.getValue();
        assertThat(payload.messageId()).isEqualTo(100L);
        assertThat(payload.roomId()).isEqualTo(10L);
        assertThat(payload.senderMemberId()).isEqualTo(5L);
        assertThat(payload.contentType()).isEqualTo(MessageContentType.TEXT);
        assertThat(payload.content()).isEqualTo("안녕하세요");
        assertThat(payload.fileMetadataIds()).isEmpty();
        assertThat(payload.createdAt()).isEqualTo(Instant.parse("2026-07-03T07:00:00Z"));
        assertThat(payload.replyToMessageId()).isNull();
    }

    @Test
    @DisplayName("파일 첨부가 있는 메시지도 올바르게 변환하여 브로드캐스트한다")
    void broadcast_파일_첨부_메시지() {
        // given
        ChatMessageCreatedEvent event = new ChatMessageCreatedEvent(
            UUID.randomUUID(),
            Instant.parse("2026-07-03T08:00:00Z"),
            200L,
            20L,
            15L,
            MessageContentType.IMAGE,
            "이미지 캡션",
            List.of("file-1", "file-2")
        );

        // when
        sut.broadcast(event);

        // then
        ArgumentCaptor<BroadcastChatMessagePayload> payloadCaptor = ArgumentCaptor.forClass(BroadcastChatMessagePayload.class);
        then(broadcastPort).should().broadcast(eq("/topic/chat/rooms/20/messages"), payloadCaptor.capture());

        BroadcastChatMessagePayload payload = payloadCaptor.getValue();
        assertThat(payload.messageId()).isEqualTo(200L);
        assertThat(payload.contentType()).isEqualTo(MessageContentType.IMAGE);
        assertThat(payload.content()).isEqualTo("이미지 캡션");
        assertThat(payload.fileMetadataIds()).containsExactly("file-1", "file-2");
    }

    @Test
    @DisplayName("답장 메시지도 답장 대상 id를 포함하여 브로드캐스트한다")
    void broadcast_답장_메시지() {
        // given
        ChatMessageCreatedEvent event = new ChatMessageCreatedEvent(
            UUID.randomUUID(),
            Instant.parse("2026-07-03T08:30:00Z"),
            300L,
            20L,
            15L,
            MessageContentType.FILE,
            "파일 답장",
            List.of("file-1"),
            200L
        );

        // when
        sut.broadcast(event);

        // then
        ArgumentCaptor<BroadcastChatMessagePayload> payloadCaptor = ArgumentCaptor.forClass(BroadcastChatMessagePayload.class);
        then(broadcastPort).should().broadcast(eq("/topic/chat/rooms/20/messages"), payloadCaptor.capture());

        BroadcastChatMessagePayload payload = payloadCaptor.getValue();
        assertThat(payload.messageId()).isEqualTo(300L);
        assertThat(payload.replyToMessageId()).isEqualTo(200L);
        assertThat(payload.contentType()).isEqualTo(MessageContentType.FILE);
        assertThat(payload.fileMetadataIds()).containsExactly("file-1");
    }

    @Test
    @DisplayName("roomId가 다른 방은 서로 다른 destination 경로를 사용한다")
    void broadcast_roomId별_독립_destination() {
        // given
        ChatMessageCreatedEvent event1 = new ChatMessageCreatedEvent(
            UUID.randomUUID(),
            Instant.now(),
            1L,
            100L,
            1L,
            MessageContentType.TEXT,
            "방 100",
            List.of()
        );

        ChatMessageCreatedEvent event2 = new ChatMessageCreatedEvent(
            UUID.randomUUID(),
            Instant.now(),
            2L,
            200L,
            1L,
            MessageContentType.TEXT,
            "방 200",
            List.of()
        );

        // when
        sut.broadcast(event1);
        sut.broadcast(event2);

        // then
        then(broadcastPort).should().broadcast(eq("/topic/chat/rooms/100/messages"), any());
        then(broadcastPort).should().broadcast(eq("/topic/chat/rooms/200/messages"), any());
    }
}
