package com.umc.product.community.application.port.in.query.thread.message.dto;

public record CommunityThreadReactionInfo(
    String emoji,
    long count,
    boolean reactedByMe
) {

    public CommunityThreadReactionInfo {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative");
        }
    }
}
