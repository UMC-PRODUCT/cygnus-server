package com.umc.product.community.adapter.in.websocket.dto.request;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.community.adapter.in.websocket.CommunityStompUuid;
import com.umc.product.community.application.port.in.command.thread.message.dto.CreateCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageType;

public record CreateCommunityThreadMessageRequest(
    @JsonProperty(value = "clientMessageId", required = true) String clientMessageId,
    @JsonProperty(value = "type", required = true) CommunityThreadMessageType type,
    String content,
    List<String> fileMetadataIds,
    List<Long> mentionedMemberIds,
    Long replyToId
) {

    private static final int MAX_CONTENT_CODE_POINTS = 2_000;
    private static final int MAX_IMAGE_COUNT = 4;
    private static final int MAX_MENTION_COUNT = 100;

    public CreateCommunityThreadMessageRequest {
        CommunityStompUuid.parseRequired(clientMessageId, "clientMessageId");
        if (type != CommunityThreadMessageType.TEXT && type != CommunityThreadMessageType.IMAGE) {
            throw new IllegalArgumentException("type must be TEXT or IMAGE");
        }

        fileMetadataIds = fileMetadataIds == null ? List.of() : fileMetadataIds;
        if (fileMetadataIds.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("fileMetadataIds must contain non-blank IDs");
        }
        if (fileMetadataIds.stream().distinct().count() != fileMetadataIds.size()) {
            throw new IllegalArgumentException("fileMetadataIds must be unique");
        }
        fileMetadataIds = List.copyOf(fileMetadataIds);

        List<Long> mentions = mentionedMemberIds == null ? List.of() : mentionedMemberIds;
        if (mentions.stream().anyMatch(value -> value == null || value <= 0)) {
            throw new IllegalArgumentException("mentionedMemberIds must contain positive IDs");
        }
        mentionedMemberIds = mentions.stream().distinct().sorted().toList();
        if (mentionedMemberIds.size() > MAX_MENTION_COUNT) {
            throw new IllegalArgumentException("mentionedMemberIds exceeds maximum size");
        }

        if (replyToId != null && replyToId <= 0) {
            throw new IllegalArgumentException("replyToId must be positive");
        }
        if (content != null && content.codePointCount(0, content.length()) > MAX_CONTENT_CODE_POINTS) {
            throw new IllegalArgumentException("content exceeds maximum length");
        }

        if (type == CommunityThreadMessageType.TEXT) {
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("TEXT content must not be blank");
            }
            if (!fileMetadataIds.isEmpty()) {
                throw new IllegalArgumentException("TEXT messages cannot contain files");
            }
        } else if (fileMetadataIds.isEmpty() || fileMetadataIds.size() > MAX_IMAGE_COUNT) {
            throw new IllegalArgumentException("IMAGE messages require one to four files");
        }
    }

    public UUID clientMessageUuid() {
        return CommunityStompUuid.parseRequired(clientMessageId, "clientMessageId");
    }

    public CreateCommunityThreadMessageCommand toCommand(Long threadId, Long senderMemberId) {
        return new CreateCommunityThreadMessageCommand(
            threadId,
            senderMemberId,
            clientMessageUuid(),
            type,
            content,
            fileMetadataIds,
            mentionedMemberIds,
            replyToId
        );
    }

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, JsonNode ignored) {
        throw new IllegalArgumentException("unknown field: " + fieldName);
    }
}
