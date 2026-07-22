package com.umc.product.chat.application.port.in.command.dto;

import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;

public record ChatMessageMutationResult(
    ChatMessageInfo message,
    boolean deduplicated
) {
}
