package com.umc.product.global.websocket.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.websocket.application.service.StompClientMessageIdResolverRegistry;
import com.umc.product.global.websocket.handler.WebSocketErrorEvent;

@DisplayName("WebSocketRateLimitInterceptor")
class WebSocketRateLimitInterceptorTest {

    private static final UUID COMMAND_ID = UUID.fromString("b108f0c7-e244-4c9d-a57a-6e5bb8e94e89");

    private final MutableClock clock = new MutableClock(Instant.parse("2026-07-18T00:00:00Z"));
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final WebSocketRateLimitInterceptor sut = new WebSocketRateLimitInterceptor(clock, eventPublisher);

    @Test
    @DisplayName("초당 20회 이하 전송 시 모든 메시지가 정상 통과된다")
    void send_within_rate_limit_passes() {
        assertThatCode(() -> {
            for (int i = 0; i < 20; i++) {
                sut.preSend(sendMessage(1L, COMMAND_ID), mock(MessageChannel.class));
            }
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("동일 멤버가 초당 21번째 메시지를 전송하면 해당 메시지만 무시한다")
    void send_exceeding_rate_limit_returns_null() {
        for (int i = 0; i < 20; i++) {
            sut.preSend(sendMessage(1L, COMMAND_ID), mock(MessageChannel.class));
        }

        Message<?> result = sut.preSend(sendMessage(1L, COMMAND_ID), mock(MessageChannel.class));

        assertThat(result).isNull();
        assertThat(eventPublisher.events).singleElement().satisfies(event -> {
            assertThat(event.commandId()).isEqualTo(COMMAND_ID);
            assertThat(event.clientMessageId()).isNull();
            assertThat(event.status()).isEqualTo(429);
            assertThat(event.code()).isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS.getCode());
            assertThat(event.message()).isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS.getMessage());
            assertThat(event.retryable()).isTrue();
        });
    }

    @Test
    @DisplayName("21번째 create SEND 오류는 handler 전 payload에서 해석한 clientMessageId를 보존한다")
    void rateLimitErrorPreservesResolvedClientMessageId() {
        UUID clientMessageId = UUID.fromString("0dce06f4-11bc-4dc2-b9fd-4f9cb88ea9cd");
        StompClientMessageIdResolverRegistry resolverRegistry =
            mock(StompClientMessageIdResolverRegistry.class);
        given(resolverRegistry.resolve(any(), any())).willReturn(Optional.of(clientMessageId));
        WebSocketRateLimitInterceptor interceptor = new WebSocketRateLimitInterceptor(
            clock,
            eventPublisher,
            List.of(),
            resolverRegistry
        );

        for (int i = 0; i < 21; i++) {
            interceptor.preSend(sendMessage(1L, COMMAND_ID), mock(MessageChannel.class));
        }

        assertThat(eventPublisher.events).singleElement()
            .extracting(WebSocketErrorEvent::clientMessageId)
            .isEqualTo(clientMessageId);
    }

    @Test
    @DisplayName("21번째 SEND 거부 후 다음 초의 명령은 같은 세션에서 통과한다")
    void nextBucketRecoversOnSameSession() {
        for (int i = 0; i < 21; i++) {
            sut.preSend(sendMessage(1L, COMMAND_ID), mock(MessageChannel.class));
        }
        clock.advanceSeconds(1);

        Message<?> result = sut.preSend(sendMessage(1L, COMMAND_ID), mock(MessageChannel.class));

        assertThat(result).isNotNull();
        assertThat(eventPublisher.events).hasSize(1);
    }

    @Test
    @DisplayName("서로 다른 멤버의 rate limit은 독립적으로 관리된다")
    void rate_limit_is_independent_per_member() {
        for (int i = 0; i < 20; i++) {
            sut.preSend(sendMessage(1L, COMMAND_ID), mock(MessageChannel.class));
        }

        assertThatCode(() -> sut.preSend(sendMessage(2L, COMMAND_ID), mock(MessageChannel.class)))
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

    private Message<byte[]> sendMessage(Long memberId, UUID commandId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/community/threads/10/messages");
        accessor.setNativeHeader("x-command-id", commandId.toString());
        MemberPrincipal principal = MemberPrincipal.builder().memberId(memberId).build();
        accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static class RecordingEventPublisher implements ApplicationEventPublisher {

        private final List<WebSocketErrorEvent> events = new ArrayList<>();

        @Override
        public void publishEvent(Object event) {
            events.add((WebSocketErrorEvent) event);
        }
    }

    private static class MutableClock extends Clock {

        private Instant instant;

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

        private void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }
    }
}
