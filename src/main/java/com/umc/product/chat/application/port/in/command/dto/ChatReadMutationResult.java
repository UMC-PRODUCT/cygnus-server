package com.umc.product.chat.application.port.in.command.dto;

public record ChatReadMutationResult(
    Long roomId,
    Long memberId,
    Long lastReadMessageId,
    boolean deduplicated
) {
}
