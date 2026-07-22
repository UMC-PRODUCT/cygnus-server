package com.umc.product.community.adapter.in.websocket.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.community.application.port.in.command.thread.message.dto.EditCommunityThreadMessageCommand;

public record EditCommunityThreadMessageRequest(
    @JsonProperty(value = "content", required = true) String content
) {

    private static final int MAX_CONTENT_CODE_POINTS = 2_000;

    public EditCommunityThreadMessageRequest {
        if (content != null && content.codePointCount(0, content.length()) > MAX_CONTENT_CODE_POINTS) {
            throw new IllegalArgumentException("content exceeds maximum length");
        }
    }

    public EditCommunityThreadMessageCommand toCommand(
        Long threadId,
        Long messageId,
        Long requesterMemberId
    ) {
        return new EditCommunityThreadMessageCommand(threadId, messageId, requesterMemberId, content);
    }

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, JsonNode ignored) {
        throw new IllegalArgumentException("unknown field: " + fieldName);
    }
}
