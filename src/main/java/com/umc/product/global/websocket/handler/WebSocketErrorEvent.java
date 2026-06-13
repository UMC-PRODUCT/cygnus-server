package com.umc.product.global.websocket.handler;

import com.umc.product.global.response.code.BaseCode;

/**
 * WebSocket 에러 응답 전송이 필요할 때 발행하는 이벤트다.
 */
public record WebSocketErrorEvent(String userName, BaseCode errorCode) {
}
