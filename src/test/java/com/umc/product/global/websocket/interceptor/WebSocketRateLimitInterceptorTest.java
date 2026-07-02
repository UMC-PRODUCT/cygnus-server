package com.umc.product.global.websocket.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.umc.product.global.security.MemberPrincipal;

@DisplayName("WebSocketRateLimitInterceptor")
class WebSocketRateLimitInterceptorTest {

    private final WebSocketRateLimitInterceptor sut = new WebSocketRateLimitInterceptor();

    @Test
    @DisplayName("초당 20회 이하 전송 시 모든 메시지가 정상 통과된다")
    void send_within_rate_limit_passes() {
        assertThatCode(() -> {
            for (int i = 0; i < 20; i++) {
                sut.preSend(sendMessage(1L), mock(MessageChannel.class));
            }
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("동일 멤버가 초당 21번째 메시지를 전송하면 해당 메시지만 무시한다")
    void send_exceeding_rate_limit_returns_null() {
        for (int i = 0; i < 20; i++) {
            sut.preSend(sendMessage(1L), mock(MessageChannel.class));
        }

        Message<?> result = sut.preSend(sendMessage(1L), mock(MessageChannel.class));

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("서로 다른 멤버의 rate limit은 독립적으로 관리된다")
    void rate_limit_is_independent_per_member() {
        for (int i = 0; i < 20; i++) {
            sut.preSend(sendMessage(1L), mock(MessageChannel.class));
        }

        assertThatCode(() -> sut.preSend(sendMessage(2L), mock(MessageChannel.class)))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SEND 외 명령어는 rate limit 없이 그대로 통과된다")
    void non_send_command_skips_rate_limit() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/test");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatCode(() -> {
            for (int i = 0; i < 100; i++) {
                sut.preSend(message, mock(MessageChannel.class));
            }
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Principal이 없는 SEND 메시지는 rate limit 없이 통과된다")
    void send_without_principal_skips_rate_limit() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/topic/test");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatCode(() -> sut.preSend(message, mock(MessageChannel.class)))
            .doesNotThrowAnyException();
    }

    private Message<byte[]> sendMessage(Long memberId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/topic/test");
        MemberPrincipal principal = MemberPrincipal.builder().memberId(memberId).build();
        accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
