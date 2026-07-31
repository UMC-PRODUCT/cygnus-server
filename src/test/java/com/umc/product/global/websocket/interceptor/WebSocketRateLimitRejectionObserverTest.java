package com.umc.product.global.websocket.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.websocket.application.port.in.WebSocketRateLimitRejectionObserver;
import com.umc.product.global.websocket.handler.WebSocketErrorEvent;

@DisplayName("WebSocket rate-limit rejection observer seam")
class WebSocketRateLimitRejectionObserverTest {

    private static final String DESTINATION = "/app/community/threads/12/messages";

    @Test
    @DisplayName("21번째 SEND 거부 시 원본 destination을 observer에 전달한다")
    void notifiesObserverWithRejectedDestination() {
        WebSocketRateLimitRejectionObserver observer = mock(WebSocketRateLimitRejectionObserver.class);
        WebSocketRateLimitInterceptor interceptor = new WebSocketRateLimitInterceptor(
            new MutableClock(Instant.parse("2026-07-18T00:00:00Z")),
            mock(ApplicationEventPublisher.class),
            List.of(observer)
        );
        MessageChannel channel = mock(MessageChannel.class);

        for (int i = 0; i < 21; i++) {
            interceptor.preSend(sendMessage(1L, DESTINATION), channel);
        }

        verify(observer).observe(DESTINATION);
    }

    @Test
    void observerFailureDoesNotChangeTypedErrorOrDrop() {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        WebSocketRateLimitRejectionObserver failingObserver = destination -> {
            throw new IllegalStateException("metric failure");
        };
        WebSocketRateLimitRejectionObserver recordingObserver = mock(WebSocketRateLimitRejectionObserver.class);
        WebSocketRateLimitInterceptor interceptor = new WebSocketRateLimitInterceptor(
            new MutableClock(Instant.parse("2026-07-18T00:00:00Z")),
            eventPublisher,
            List.of(failingObserver, recordingObserver)
        );
        MessageChannel channel = mock(MessageChannel.class);
        Message<?> rejected = null;

        for (int i = 0; i < 21; i++) {
            rejected = interceptor.preSend(sendMessage(1L, DESTINATION), channel);
        }

        assertThat(rejected).isNull();
        verify(eventPublisher).publishEvent(any(WebSocketErrorEvent.class));
        verify(recordingObserver).observe(DESTINATION);
    }

    private Message<byte[]> sendMessage(Long memberId, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination(destination);
        MemberPrincipal principal = MemberPrincipal.builder().memberId(memberId).build();
        accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static final class MutableClock extends Clock {

        private final Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
