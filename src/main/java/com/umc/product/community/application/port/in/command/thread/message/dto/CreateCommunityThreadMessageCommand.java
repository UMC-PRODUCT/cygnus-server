package com.umc.product.community.application.port.in.command.thread.message.dto;

import java.util.List;
import java.util.UUID;

import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageType;

public record CreateCommunityThreadMessageCommand(
    Long threadId,
    Long senderMemberId,
    UUID clientMessageId,
    CommunityThreadMessageType type,
    String content,
    List<String> fileMetadataIds,
    List<Long> mentionedMemberIds,
    Long replyToMessageId
) {

    public CreateCommunityThreadMessageCommand {
        threadId = positiveId(threadId, "threadId");
        senderMemberId = positiveId(senderMemberId, "senderMemberId");
        if (clientMessageId == null) {
            throw new IllegalArgumentException("clientMessageId must not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        fileMetadataIds = fileMetadataIds == null ? List.of() : List.copyOf(fileMetadataIds);

        List<Long> mentions = mentionedMemberIds == null ? List.of() : mentionedMemberIds;
        if (mentions.stream().anyMatch(memberId -> memberId == null || memberId <= 0)) {
            throw new IllegalArgumentException("mentionedMemberIds must contain positive IDs");
        }
        mentionedMemberIds = List.copyOf(mentions.stream().distinct().sorted().toList());

        if (replyToMessageId != null) {
            replyToMessageId = positiveId(replyToMessageId, "replyToMessageId");
        }
    }

    private static Long positiveId(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
