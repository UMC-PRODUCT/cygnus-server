package com.umc.product.chat.domain.event;

import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.global.event.domain.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 채팅 메시지가 생성되었음을 알리는 도메인 이벤트.
 * <p>
 * chat 도메인은 이 이벤트를 발행하기만 하고, 누가 수신하는지는 알지 못한다.
 * <ul>
 *     <li>실시간 broadcast — STOMP broadcast 어댑터가 수신.</li>
 *     <li>문의 상태 전환 — inquiry 도메인이 수신하여 운영진 첫 메시지 시 진행 전환 등을 처리.</li>
 * </ul>
 * <p>
 * 수신 측이 DB 재조회 없이 이벤트만으로 WebSocket 응답(메시지 전체)을 구성할 수 있도록,
 * 생성된 메시지의 모든 필드(첨부 {@code fileMetadataIds} 포함)를 그대로 담는다.
 */
public record ChatMessageCreatedEvent(
    UUID eventId,
    Instant occurredAt,
    Long messageId,
    Long roomId,
    Long senderMemberId,
    MessageContentType contentType,
    String content,
    List<String> fileMetadataIds
) implements DomainEvent {

    public static ChatMessageCreatedEvent from(ChatMessage message) {
        return new ChatMessageCreatedEvent(
            UUID.randomUUID(),
            message.getCreatedAt(),
            message.getId(),
            message.getRoomId(),
            message.getSenderMemberId(),
            message.getContentType(),
            message.getContent(),
            message.getFileMetadataIds()
        );
    }

    @Override
    public String eventType() {
        return "chat.message.created";
    }
}
