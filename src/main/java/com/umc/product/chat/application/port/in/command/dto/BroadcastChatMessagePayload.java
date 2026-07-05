package com.umc.product.chat.application.port.in.command.dto;

import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;
import java.time.Instant;
import java.util.List;

/**
 * 채팅 메시지 broadcast 전용 payload.
 * <p>
 * query 조회 모델({@code ChatMessageInfo})과 broadcast payload 의 결합을 끊기 위해 분리한 타입이다.
 * 클라이언트가 수신하는 JSON 필드 구조는 기존과 동일하다.
 */
public record BroadcastChatMessagePayload(
    Long messageId,
    Long roomId,
    Long senderMemberId,
    MessageContentType contentType,
    String content,
    List<String> fileMetadataIds,
    Instant createdAt
) {

    public static BroadcastChatMessagePayload from(ChatMessageCreatedEvent event) {
        return new BroadcastChatMessagePayload(
            event.messageId(),
            event.roomId(),
            event.senderMemberId(),
            event.contentType(),
            event.content(),
            event.fileMetadataIds(),
            event.occurredAt()
        );
    }
}
