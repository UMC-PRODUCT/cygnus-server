package com.umc.product.chat.domain.event;

import java.time.Instant;
import java.util.UUID;

import com.umc.product.global.event.domain.DomainEvent;

/**
 * 채팅방의 고정 메시지 참조가 변경됐음을 알리는 도메인 이벤트.
 * <p>
 * 소비 도메인은 이 이벤트를 자신의 topic 으로 전달하고, 클라이언트는 소비 도메인의 방 상세 API 를 재조회해
 * nullable {@code pinnedMessageId}에 대응하는 전체 고정 메시지를 복원한다.
 */
public record ChatRoomPinnedMessageChangedEvent(
    UUID eventId,
    Instant occurredAt,
    Long roomId,
    Long pinnedMessageId
) implements DomainEvent {

    public ChatRoomPinnedMessageChangedEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public static ChatRoomPinnedMessageChangedEvent of(Long roomId, Long pinnedMessageId) {
        return new ChatRoomPinnedMessageChangedEvent(null, null, roomId, pinnedMessageId);
    }

    @Override
    public String eventType() {
        return "chat.room.pinned-message.changed";
    }
}
