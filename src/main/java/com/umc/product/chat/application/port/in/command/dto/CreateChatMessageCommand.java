package com.umc.product.chat.application.port.in.command.dto;

import java.util.List;
import java.util.UUID;

import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

public record CreateChatMessageCommand(
    Long roomId,
    Long senderMemberId,
    UUID clientMessageId,
    MessageContentType contentType,
    String content,
    List<String> fileMetadataIds,
    List<Long> mentionedMemberIds,
    Long replyToMessageId
) {
    public CreateChatMessageCommand {
        List<String> files = fileMetadataIds == null ? List.of() : fileMetadataIds;
        if (files.stream().anyMatch(fileId -> fileId == null || fileId.isBlank())) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_INVALID_ATTACHMENT);
        }
        fileMetadataIds = List.copyOf(files);

        List<Long> mentions = mentionedMemberIds == null ? List.of() : mentionedMemberIds;
        if (mentions.stream().anyMatch(memberId -> memberId == null || memberId <= 0)) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_INVALID_MENTION);
        }
        mentionedMemberIds = mentions.stream().distinct().sorted().toList();
    }
}
