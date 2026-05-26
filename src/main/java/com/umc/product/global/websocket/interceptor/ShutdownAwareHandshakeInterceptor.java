package com.umc.product.global.websocket.interceptor;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Slf4j
@Component
public class ShutdownAwareHandshakeInterceptor implements HandshakeInterceptor, SmartLifecycle {

    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        if (shuttingDown.get()) {
            log.warn("서버 종료 중 - 신규 WebSocket 핸드셰이크 거부: {}", request.getRemoteAddress());
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return false;
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        log.info("서버 종료 감지 - 신규 WebSocket 연결을 거부합니다.");
        shuttingDown.set(true);
    }

    @Override
    public boolean isRunning() {
        return !shuttingDown.get();
    }

    @Override
    public int getPhase() {
        return SmartLifecycle.DEFAULT_PHASE;
    }
}
