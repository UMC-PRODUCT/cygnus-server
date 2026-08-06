package com.umc.product.community.adapter.in.websocket;

import java.util.UUID;

record CommunityStompCommandOutcome(
    CommunityStompCommandType command,
    Long messageId,
    UUID clientMessageId,
    boolean deduplicated
) {
}
