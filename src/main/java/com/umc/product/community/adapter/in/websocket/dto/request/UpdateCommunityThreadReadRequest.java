package com.umc.product.community.adapter.in.websocket.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.community.application.port.in.command.thread.message.dto.UpdateCommunityThreadReadCommand;

public record UpdateCommunityThreadReadRequest(
    @JsonProperty(value = "lastReadMessageId", required = true) Long lastReadMessageId
) {

    public UpdateCommunityThreadReadRequest {
        if (lastReadMessageId == null || lastReadMessageId <= 0) {
            throw new IllegalArgumentException("lastReadMessageId must be positive");
        }
    }

    public UpdateCommunityThreadReadCommand toCommand(Long threadId, Long memberId) {
        return new UpdateCommunityThreadReadCommand(threadId, memberId, lastReadMessageId);
    }

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, JsonNode ignored) {
        throw new IllegalArgumentException("unknown field: " + fieldName);
    }
}
