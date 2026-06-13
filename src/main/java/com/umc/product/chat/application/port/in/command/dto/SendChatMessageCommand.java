package com.umc.product.chat.application.port.in.command.dto;

import com.umc.product.chat.domain.MessageContentType;
import java.util.List;

public record SendChatMessageCommand(
    Long roomId,
    Long senderMemberId,
    MessageContentType contentType,
    String content,
    List<String> fileMetadataIds
) {
}
