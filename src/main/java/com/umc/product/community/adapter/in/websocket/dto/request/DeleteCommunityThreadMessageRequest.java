package com.umc.product.community.adapter.in.websocket.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.community.application.port.in.command.thread.message.dto.TombstoneCommunityThreadMessageCommand;

public record DeleteCommunityThreadMessageRequest() {

    public TombstoneCommunityThreadMessageCommand toCommand(
        Long threadId,
        Long messageId,
        Long requesterMemberId
    ) {
        return new TombstoneCommunityThreadMessageCommand(threadId, messageId, requesterMemberId);
    }

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, JsonNode ignored) {
        throw new IllegalArgumentException("unknown field: " + fieldName);
    }
}
