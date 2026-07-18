package com.umc.product.global.websocket.handler;

import java.util.UUID;

import org.springframework.lang.Nullable;

import com.umc.product.global.response.code.BaseCode;

/**
 * WebSocket 에러 응답 전송이 필요할 때 발행하는 이벤트다.
 */
public record WebSocketErrorEvent(
    String userName,
    @Nullable UUID commandId,
    @Nullable UUID clientMessageId,
    int status,
    String code,
    String message,
    boolean retryable
) {

    public static WebSocketErrorEvent from(
        String userName,
        @Nullable UUID commandId,
        @Nullable UUID clientMessageId,
        BaseCode errorCode,
        boolean retryable
    ) {
        return new WebSocketErrorEvent(
            userName,
            commandId,
            clientMessageId,
            errorCode.getHttpStatus().value(),
            errorCode.getCode(),
            errorCode.getMessage(),
            retryable
        );
    }
}
