package com.umc.product.global.config;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.support.ChannelInterceptor;

import com.umc.product.global.websocket.handler.ApiResponseStompErrorHandler;
import com.umc.product.global.websocket.interceptor.ShutdownAwareHandshakeInterceptor;
import com.umc.product.global.websocket.interceptor.StompAuthChannelInterceptor;
import com.umc.product.global.websocket.interceptor.StompPrincipalInterceptor;
import com.umc.product.global.websocket.interceptor.WebSocketInboundMetricInterceptor;
import com.umc.product.global.websocket.interceptor.WebSocketOutboundMetricInterceptor;
import com.umc.product.global.websocket.interceptor.WebSocketRateLimitInterceptor;
import com.umc.product.global.websocket.relay.RelayDestinationChannelInterceptors;

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
    ChannelInterceptor toBrokerDestinationInterceptor;

    @Mock
    ChannelInterceptor fromBrokerDestinationInterceptor;

    @InjectMocks
    WebSocketMessageBrokerConfig sut;

    @Test
    @DisplayName(
        "inbound 체인은 principal 다음 rate-limit을 실행해 auth DB 조회와 metric보다 21번째 SEND를 먼저 차단한다"
    )
    void configureInboundInterceptorsInFailFastOrder() {
        ChannelRegistration registration = mock(ChannelRegistration.class, Answers.RETURNS_DEEP_STUBS);
        given(relayDestinationChannelInterceptors.toBroker())
            .willReturn(toBrokerDestinationInterceptor);

        sut.configureClientInboundChannel(registration);

        verify(registration).interceptors(
            stompPrincipalInterceptor,
            webSocketRateLimitInterceptor,
            stompAuthChannelInterceptor,
            webSocketInboundMetricInterceptor,
            toBrokerDestinationInterceptor
        );
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
}
