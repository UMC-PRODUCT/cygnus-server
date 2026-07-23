package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.SimpleBrokerRegistration;
import org.springframework.messaging.simp.config.StompBrokerRelayRegistration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import com.umc.product.global.websocket.handler.ApiResponseStompErrorHandler;
import com.umc.product.global.websocket.interceptor.ShutdownAwareHandshakeInterceptor;
import com.umc.product.global.websocket.interceptor.StompAuthChannelInterceptor;
import com.umc.product.global.websocket.interceptor.StompPrincipalInterceptor;
import com.umc.product.global.websocket.interceptor.WebSocketInboundMetricInterceptor;
import com.umc.product.global.websocket.interceptor.WebSocketOutboundMetricInterceptor;
import com.umc.product.global.websocket.interceptor.WebSocketRateLimitInterceptor;
import com.umc.product.global.websocket.relay.RelayDestinationChannelInterceptors;
import com.umc.product.global.websocket.session.AccessTokenWebSocketSessionRegistry;
import com.umc.product.term.adapter.in.websocket.TermConsentStompInterceptor;

import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.ObservationRegistry;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketMessageBrokerConfig")
class WebSocketMessageBrokerConfigTest {

    @Mock
    StompPrincipalInterceptor stompPrincipalInterceptor;

    @Mock
    StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Mock
    TermConsentStompInterceptor termConsentStompInterceptor;

    @Mock
    ObjectProvider<TermConsentStompInterceptor> termConsentStompInterceptorProvider;

    @Mock
    WebSocketRateLimitInterceptor webSocketRateLimitInterceptor;

    @Mock
    WebSocketInboundMetricInterceptor webSocketInboundMetricInterceptor;

    @Mock
    WebSocketOutboundMetricInterceptor webSocketOutboundMetricInterceptor;

    @Mock
    ShutdownAwareHandshakeInterceptor shutdownAwareHandshakeInterceptor;

    @Mock
    ApiResponseStompErrorHandler apiResponseStompErrorHandler;

    @Mock
    ObservationRegistry observationRegistry;

    @Mock
    ContextSnapshotFactory snapshotFactory;

    @Mock
    WebSocketBrokerProperties brokerProperties;

    @Mock
    Environment environment;

    @Mock
    RelayDestinationChannelInterceptors relayDestinationChannelInterceptors;

    @Mock
    AccessTokenWebSocketSessionRegistry accessTokenWebSocketSessionRegistry;

    @Mock
    ObjectProvider<AccessTokenWebSocketSessionRegistry> accessTokenWebSocketSessionRegistryProvider;

    @Mock
    MessageBrokerRegistry registry;

    @Mock
    SimpleBrokerRegistration simpleBrokerRegistration;

    @Mock
    StompBrokerRelayRegistration relayRegistration;

    @Mock
    ChannelRegistration brokerChannelRegistration;

    @Mock
    ChannelInterceptor toBrokerDestinationInterceptor;

    @Mock
    ChannelInterceptor fromBrokerDestinationInterceptor;

    WebSocketMessageBrokerConfig sut;

    @BeforeEach
    void setUp() {
        sut = new WebSocketMessageBrokerConfig(
            stompPrincipalInterceptor,
            stompAuthChannelInterceptor,
            termConsentStompInterceptorProvider,
            webSocketRateLimitInterceptor,
            webSocketInboundMetricInterceptor,
            webSocketOutboundMetricInterceptor,
            shutdownAwareHandshakeInterceptor,
            apiResponseStompErrorHandler,
            observationRegistry,
            snapshotFactory,
            brokerProperties,
            environment,
            relayDestinationChannelInterceptors,
            accessTokenWebSocketSessionRegistryProvider
        );
    }

    @Test
    @DisplayName(
        "inbound 체인은 principal 다음 rate-limit을 실행해 auth DB 조회와 metric보다 21번째 SEND를 먼저 차단한다"
    )
    void configureInboundInterceptorsInFailFastOrder() {
        ChannelRegistration registration = mock(ChannelRegistration.class, Answers.RETURNS_DEEP_STUBS);
        given(relayDestinationChannelInterceptors.toBroker())
            .willReturn(toBrokerDestinationInterceptor);
        given(termConsentStompInterceptorProvider.getIfAvailable()).willReturn(termConsentStompInterceptor);

        sut.configureClientInboundChannel(registration);

        verify(registration).interceptors(
            stompPrincipalInterceptor,
            webSocketRateLimitInterceptor,
            termConsentStompInterceptor,
            stompAuthChannelInterceptor,
            webSocketInboundMetricInterceptor,
            toBrokerDestinationInterceptor
        );
    }

    @Test
    @DisplayName("약관 강제가 활성화되면 STOMP interceptor 누락을 시작 단계에서 거부한다")
    void rejectMissingTermConsentInterceptorWhenEnabled() {
        ChannelRegistration registration = mock(ChannelRegistration.class, Answers.RETURNS_DEEP_STUBS);
        given(environment.getProperty("app.terms.reconsent.enabled", Boolean.class, false)).willReturn(true);

        assertThatThrownBy(() -> sut.configureClientInboundChannel(registration))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("TermConsentStompInterceptor");
    }

    @Test
    @DisplayName("약관 강제가 활성화되면 WebSocket session registry 누락을 시작 단계에서 거부한다")
    void rejectMissingSessionRegistryWhenEnabled() {
        WebSocketTransportRegistration registration = mock(WebSocketTransportRegistration.class);
        given(environment.getProperty("app.terms.reconsent.enabled", Boolean.class, false)).willReturn(true);

        assertThatThrownBy(() -> sut.configureWebSocketTransport(registration))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AccessTokenWebSocketSessionRegistry");
    }

    @Test
    @DisplayName("outbound 체인은 공개 destination 복원 후 metric과 client 전송을 수행한다")
    void configureOutboundDestinationRestoreBeforeMetric() {
        ChannelRegistration registration = mock(ChannelRegistration.class, Answers.RETURNS_DEEP_STUBS);
        given(relayDestinationChannelInterceptors.fromBroker())
            .willReturn(fromBrokerDestinationInterceptor);

        sut.configureClientOutboundChannel(registration);

        verify(registration).interceptors(
            fromBrokerDestinationInterceptor,
            webSocketOutboundMetricInterceptor
        );
    }

    @Test
    @DisplayName("simple mode는 인메모리 topic과 queue broker를 구성한다")
    void configureSimpleBroker() {
        given(brokerProperties.mode()).willReturn(WebSocketBrokerProperties.Mode.SIMPLE);
        given(registry.enableSimpleBroker("/topic", "/queue")).willReturn(simpleBrokerRegistration);
        given(registry.configureBrokerChannel()).willReturn(brokerChannelRegistration);
        given(relayDestinationChannelInterceptors.toBroker())
            .willReturn(toBrokerDestinationInterceptor);

        sut.configureMessageBroker(registry);

        verify(brokerChannelRegistration).interceptors(toBrokerDestinationInterceptor);
        verify(simpleBrokerRegistration).setHeartbeatValue(new long[]{4000, 4000});
        verify(simpleBrokerRegistration).setTaskScheduler(any(ThreadPoolTaskScheduler.class));
        verify(registry).setApplicationDestinationPrefixes("/app");
        verifyNoInteractions(relayRegistration);
    }

    @Test
    @DisplayName("relay mode는 외부 STOMP broker와 다중 인스턴스 user destination을 구성한다")
    void configureRelayBroker() {
        WebSocketBrokerProperties.Relay relay = relayProperties();
        given(brokerProperties.mode()).willReturn(WebSocketBrokerProperties.Mode.RELAY);
        given(brokerProperties.relay()).willReturn(relay);
        given(registry.enableStompBrokerRelay("/topic", "/queue")).willReturn(relayRegistration);
        given(registry.configureBrokerChannel()).willReturn(brokerChannelRegistration);
        given(relayDestinationChannelInterceptors.toBroker())
            .willReturn(toBrokerDestinationInterceptor);

        sut.configureMessageBroker(registry);

        verify(brokerChannelRegistration).interceptors(toBrokerDestinationInterceptor);
        verify(relayRegistration).setRelayHost(relay.host());
        verify(relayRegistration).setRelayPort(relay.resolvedPort());
        verify(relayRegistration).setVirtualHost(relay.virtualHost());
        verify(relayRegistration).setSystemLogin(relay.systemLogin());
        verify(relayRegistration).setSystemPasscode(relay.systemPassword());
        verify(relayRegistration).setClientLogin(relay.clientLogin());
        verify(relayRegistration).setClientPasscode(relay.clientPassword());
        verify(relayRegistration).setTcpClient(any());
        verify(relayRegistration).setSystemHeartbeatSendInterval(10_000L);
        verify(relayRegistration).setSystemHeartbeatReceiveInterval(10_000L);
        verify(relayRegistration).setTaskScheduler(any(ThreadPoolTaskScheduler.class));
        verify(relayRegistration).setUserDestinationBroadcast("/topic/__internal.user-destination");
        verify(relayRegistration).setUserRegistryBroadcast("/topic/__internal.user-registry");
        verify(registry).setApplicationDestinationPrefixes("/app");
        verifyNoInteractions(simpleBrokerRegistration);
    }

    private WebSocketBrokerProperties.Relay relayProperties() {
        return new WebSocketBrokerProperties.Relay(
            "broker.internal",
            61613,
            true,
            "/product",
            "system-user",
            "system-password",
            "client-user",
            "client-password",
            Duration.ofSeconds(10),
            Duration.ofSeconds(10),
            Duration.ofSeconds(5)
        );
    }
}
