package com.umc.product.chat.application.port.in.query.dto;

import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.MessageContentType;
import java.time.Instant;
import java.util.List;

/**
 * 채팅 메시지 조회 모델. 전송 결과 및 내역/미리보기 조회에 공통으로 사용한다.
 */
public record ChatMessageInfo(
    Long messageId,
    Long roomId,
    Long senderMemberId,
    MessageContentType contentType,
    String content,
    List<String> fileMetadataIds,
    Instant createdAt
) {
    public static ChatMessageInfo from(ChatMessage message) {
        return new ChatMessageInfo(
            message.getId(),
            message.getRoomId(),
            message.getSenderMemberId(),
            message.getContentType(),
            message.getContent(),
            message.getFileMetadataIds(),
            message.getCreatedAt()
        );
    }
}
