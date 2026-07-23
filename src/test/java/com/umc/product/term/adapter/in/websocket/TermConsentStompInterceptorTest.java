package com.umc.product.term.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.websocket.handler.ApiResponseStompErrorHandler;
import com.umc.product.global.websocket.session.AccessTokenWebSocketSessionRegistry;
import com.umc.product.term.config.TermConsentEnforcementProperties;
import com.umc.product.term.domain.exception.TermDomainException;

class TermConsentStompInterceptorTest {

    private static final Instant NOW = Instant.parse("2026-07-23T00:00:00Z");
    private final OperationalMetrics operationalMetrics = mock(OperationalMetrics.class);
    private final AccessTokenWebSocketSessionRegistry sessionRegistry =
        mock(AccessTokenWebSocketSessionRegistry.class);
    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    @DisplayName("미동의 CONNECT는 기존 ERROR frame 형식으로 TERMS-0012를 반환한다")
    void blockConnectWithErrorFrame() {
        Message<byte[]> connect = message(StompCommand.CONNECT, principal(false, NOW.plusSeconds(3600)));
        TermConsentStompInterceptor sut = interceptor(true);

        Throwable exception = org.assertj.core.api.Assertions.catchThrowable(
            () -> sut.preSend(connect, channel)
        );
        Message<byte[]> errorFrame = new ApiResponseStompErrorHandler(new ObjectMapper())
            .handleClientMessageProcessingError(connect, exception);

        assertThat(exception).isInstanceOf(TermDomainException.class);
        assertThat(StompHeaderAccessor.wrap(errorFrame).getCommand()).isEqualTo(StompCommand.ERROR);
        assertThat(new String(errorFrame.getPayload(), StandardCharsets.UTF_8)).contains("TERMS-0012");
        then(operationalMetrics).should()
            .recordSecurityEvent("terms", "reconsent_enforcement", "blocked_stomp");
    }

    @Test
    @DisplayName("미동의 SEND와 SUBSCRIBE도 차단한다")
    void blockSendAndSubscribe() {
        TermConsentStompInterceptor sut = interceptor(true);

        assertThatThrownBy(() -> sut.preSend(
            message(StompCommand.SEND, principal(false, NOW.plusSeconds(3600))), channel
        )).isInstanceOf(TermDomainException.class);
        assertThatThrownBy(() -> sut.preSend(
            message(StompCommand.SUBSCRIBE, principal(false, NOW.plusSeconds(3600))), channel
        )).isInstanceOf(TermDomainException.class);
    }

    @Test
    @DisplayName("동의 완료 CONNECT는 토큰 만료 시각에 세션 종료를 예약한다")
    void scheduleAgreedSessionExpiry() {
        Instant expiresAt = NOW.plusSeconds(3600);
        Message<byte[]> connect = message(StompCommand.CONNECT, principal(true, expiresAt));

        Message<?> result = interceptor(true).preSend(connect, channel);

        assertThat(result).isSameAs(connect);
        then(sessionRegistry).should().scheduleExpiry("session-1", expiresAt);
    }

    @Test
    @DisplayName("이미 만료된 AccessToken의 STOMP 명령은 거부한다")
    void rejectExpiredToken() {
        Message<byte[]> send = message(StompCommand.SEND, principal(true, NOW));

        assertThatThrownBy(() -> interceptor(true).preSend(send, channel))
            .isInstanceOf(AuthenticationDomainException.class);
    }

    @Test
    @DisplayName("feature flag가 비활성이면 약관 상태를 검사하지 않는다")
    void passWhenDisabled() {
        Message<byte[]> connect = message(StompCommand.CONNECT, principal(false, NOW.plusSeconds(3600)));

        assertThat(interceptor(false).preSend(connect, channel)).isSameAs(connect);
        then(sessionRegistry).shouldHaveNoInteractions();
    }

    private TermConsentStompInterceptor interceptor(boolean enabled) {
        return new TermConsentStompInterceptor(
            new TermConsentEnforcementProperties(enabled),
            Clock.fixed(NOW, ZoneOffset.UTC),
            operationalMetrics,
            sessionRegistry
        );
    }

    private MemberPrincipal principal(boolean requiredTermsAgreed, Instant expiresAt) {
        return MemberPrincipal.builder()
            .memberId(100L)
            .requiredTermsAgreed(requiredTermsAgreed)
            .accessTokenExpiresAt(expiresAt)
            .build();
    }

    private Message<byte[]> message(StompCommand command, MemberPrincipal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId("session-1");
        accessor.setUser(new UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.getAuthorities()
        ));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
