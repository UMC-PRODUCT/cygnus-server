package com.umc.product.chat.adapter.in.web.dto.request;

import com.umc.product.chat.application.port.in.command.dto.SendChatMessageCommand;
import com.umc.product.chat.domain.MessageContentType;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SendChatMessageRequest(
    @NotNull(message = "콘텐츠 타입은 필수입니다.")
    MessageContentType contentType,
    String content,
    List<String> fileMetadataIds
) {
    public SendChatMessageCommand toCommand(Long roomId, Long senderMemberId) {
        return new SendChatMessageCommand(roomId, senderMemberId, contentType, content, fileMetadataIds);
    }
}
