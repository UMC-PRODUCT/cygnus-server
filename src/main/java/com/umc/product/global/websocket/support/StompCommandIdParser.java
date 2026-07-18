package com.umc.product.global.websocket.support;

import java.util.UUID;

import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

public final class StompCommandIdParser {

    public static final String COMMAND_ID_HEADER = "x-command-id";

    private StompCommandIdParser() {
    }

    @Nullable public static UUID parse(StompHeaderAccessor accessor) {
        String rawCommandId = accessor.getFirstNativeHeader(COMMAND_ID_HEADER);
        if (rawCommandId == null) {
            return null;
        }

        try {
            UUID commandId = UUID.fromString(rawCommandId);
            return commandId.toString().equals(rawCommandId) ? commandId : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
