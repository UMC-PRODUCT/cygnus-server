package com.umc.product.community.adapter.in.websocket.dto.request;

import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.community.application.port.in.command.thread.message.dto.ChangeCommunityThreadMessageReactionCommand;

public record ChangeCommunityThreadReactionRequest(
    @JsonProperty(value = "emoji", required = true) String emoji
) {

    private static final Pattern SINGLE_GRAPHEME = Pattern.compile("\\X");
    private static final int MAX_CODE_POINTS = 32;

    public ChangeCommunityThreadReactionRequest {
        if (emoji == null
            || emoji.isBlank()
            || emoji.codePointCount(0, emoji.length()) > MAX_CODE_POINTS
            || !SINGLE_GRAPHEME.matcher(emoji).matches()) {
            throw new IllegalArgumentException("emoji must be one extended grapheme cluster");
        }
    }

    public ChangeCommunityThreadMessageReactionCommand toCommand(
        Long threadId,
        Long messageId,
        Long memberId
    ) {
        return new ChangeCommunityThreadMessageReactionCommand(threadId, messageId, memberId, emoji);
    }

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, JsonNode ignored) {
        throw new IllegalArgumentException("unknown field: " + fieldName);
    }
}
