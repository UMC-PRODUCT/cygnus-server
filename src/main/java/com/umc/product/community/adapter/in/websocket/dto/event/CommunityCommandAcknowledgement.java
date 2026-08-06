package com.umc.product.community.adapter.in.websocket.dto.event;

import java.util.Objects;
import java.util.UUID;

import org.springframework.lang.Nullable;

import com.umc.product.community.adapter.in.websocket.CommunityStompCommandType;

public record CommunityCommandAcknowledgement(
    UUID commandId,
    CommunityStompCommandType command,
    @Nullable Long messageId,
    @Nullable UUID clientMessageId,
    boolean deduplicated
) {

    public CommunityCommandAcknowledgement {
        Objects.requireNonNull(commandId, "commandId must not be null");
        Objects.requireNonNull(command, "command must not be null");
        if (messageId != null && messageId <= 0) {
            throw new IllegalArgumentException("messageId must be positive");
        }
    }
}
