package com.umc.product.chat.application.port.in.command.dto;

import java.util.List;

import com.umc.product.chat.application.port.in.query.dto.ChatReactionInfo;

public record ChatReactionMutationResult(
    Long messageId,
    List<ChatReactionInfo> reactions,
    boolean deduplicated
) {
    public ChatReactionMutationResult {
        reactions = List.copyOf(reactions);
    }
}
