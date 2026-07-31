package com.umc.product.global.websocket.handler;

import java.util.UUID;

import org.springframework.lang.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public record WebSocketErrorPayload(
    @Nullable UUID commandId,
    @Nullable UUID clientMessageId,
    @JsonSerialize(using = WebSocketHttpStatusSerializer.class) int status,
    String code,
    String message,
    boolean retryable
) {

    public static WebSocketErrorPayload from(WebSocketErrorEvent event) {
        return new WebSocketErrorPayload(
            event.commandId(),
            event.clientMessageId(),
            event.status(),
            event.code(),
            event.message(),
            event.retryable()
        );
    }
}
