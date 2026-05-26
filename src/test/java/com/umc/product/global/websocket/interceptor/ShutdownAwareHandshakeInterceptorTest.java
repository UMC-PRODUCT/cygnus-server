package com.umc.product.global.websocket.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

@DisplayName("ShutdownAwareHandshakeInterceptor")
@ExtendWith(MockitoExtension.class)
class ShutdownAwareHandshakeInterceptorTest {

    private final ShutdownAwareHandshakeInterceptor sut = new ShutdownAwareHandshakeInterceptor();
    @Mock
    private ServerHttpRequest request;
    @Mock
    private ServerHttpResponse response;
    @Mock
    private WebSocketHandler wsHandler;

    @Test
    @DisplayName("정상 상태에서 핸드셰이크 요청이 허용된다")
    void handshake_is_allowed_when_running() {
        assertThat(sut.beforeHandshake(request, response, wsHandler, new HashMap<>())).isTrue();
    }

    @Test
    @DisplayName("stop 호출 후 핸드셰이크 요청이 거부되고 503 상태가 설정된다")
    void handshake_is_rejected_with_503_after_stop() {
        sut.stop();

        assertThat(sut.beforeHandshake(request, response, wsHandler, new HashMap<>())).isFalse();
        verify(response).setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("stop 호출 전 isRunning이 true를 반환한다")
    void is_running_returns_true_before_stop() {
        assertThat(sut.isRunning()).isTrue();
    }

    @Test
    @DisplayName("stop 호출 후 isRunning이 false를 반환한다")
    void is_running_returns_false_after_stop() {
        sut.stop();

        assertThat(sut.isRunning()).isFalse();
    }

    @Test
    @DisplayName("getPhase는 SmartLifecycle.DEFAULT_PHASE를 반환한다")
    void get_phase_returns_default_phase() {
        assertThat(sut.getPhase()).isEqualTo(SmartLifecycle.DEFAULT_PHASE);
    }
}
