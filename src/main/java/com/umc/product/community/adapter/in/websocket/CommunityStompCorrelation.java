package com.umc.product.community.adapter.in.websocket;

import java.util.Objects;
import java.util.UUID;

import org.springframework.lang.Nullable;

public record CommunityStompCorrelation(
    String userName,
    UUID commandId,
    @Nullable UUID clientMessageId,
    CommunityStompCommandType command
) {

    public CommunityStompCorrelation {
        Objects.requireNonNull(userName, "userName must not be null");
        Objects.requireNonNull(commandId, "commandId must not be null");
        Objects.requireNonNull(command, "command must not be null");
    }
}
