package com.umc.product.global.websocket.application.port.in;

import org.springframework.lang.Nullable;

@FunctionalInterface
public interface WebSocketRateLimitRejectionObserver {

    void observe(@Nullable String destination);
}
