package com.umc.product.inquiry.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.List;

import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.domain.MessageContentType;

public record InquiryMessageResponse(
    Long messageId,
    Long roomId,
    Long senderMemberId,
    MessageContentType contentType,
    String content,
    List<String> fileMetadataIds,
    Instant createdAt,
    Long replyToMessageId
) {
    public static InquiryMessageResponse from(ChatMessageInfo info) {
        return new InquiryMessageResponse(
            info.messageId(),
            info.roomId(),
            info.senderMemberId(),
            info.contentType(),
            info.content(),
            info.fileMetadataIds(),
            info.createdAt(),
            info.replyToMessageId()
        );
    }
}
