package com.umc.product.chat.application.port.in.query.dto;

public record ChatReactionInfo(
    String emoji,
    long count,
    boolean reactedByMe
) {
}
