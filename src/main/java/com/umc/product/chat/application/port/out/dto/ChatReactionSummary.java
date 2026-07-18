package com.umc.product.chat.application.port.out.dto;

public record ChatReactionSummary(
    Long messageId,
    String emoji,
    long count,
    boolean reactedByViewer
) {
}
