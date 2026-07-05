package com.umc.product.chat.adapter.in.web.dto.response;

import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;
import java.time.Instant;
import java.util.List;

public record ChatMessageResponse(
    Long messageId,
    Long roomId,
    Long senderMemberId,
    MessageContentType contentType,
    String content,
    List<String> fileMetadataIds,
    Instant createdAt
) {
    public static ChatMessageResponse from(ChatMessageInfo info) {
        return new ChatMessageResponse(
            info.messageId(),
            info.roomId(),
            info.senderMemberId(),
            info.contentType(),
            info.content(),
            info.fileMetadataIds(),
            info.createdAt()
        );
    }

    public static ChatMessageResponse from(ChatMessageCreatedEvent event) {
        return new ChatMessageResponse(
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
