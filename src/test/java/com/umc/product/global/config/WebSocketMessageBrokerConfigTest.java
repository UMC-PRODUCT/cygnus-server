package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskDecorator;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.SimpleBrokerRegistration;
import org.springframework.messaging.simp.config.StompBrokerRelayRegistration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.global.websocket.handler.ApiResponseStompErrorHandler;
import com.umc.product.global.websocket.interceptor.ShutdownAwareHandshakeInterceptor;
import com.umc.product.global.websocket.interceptor.StompAuthChannelInterceptor;
import com.umc.product.global.websocket.interceptor.StompPrincipalInterceptor;
import com.umc.product.global.websocket.interceptor.WebSocketInboundMetricInterceptor;
import com.umc.product.global.websocket.interceptor.WebSocketOutboundMetricInterceptor;
import com.umc.product.global.websocket.interceptor.WebSocketRateLimitInterceptor;
import com.umc.product.global.websocket.relay.RelayDestinationChannelInterceptors;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.Observation;
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

    @Test
    @DisplayName("outbound 작업은 현재 observation이 있으면 child observation 안에서 실행한다")
    void outboundTaskWithParentObservation() {
        ContextSnapshotFactory localSnapshotFactory = mock(ContextSnapshotFactory.class);
        ContextSnapshot snapshot = mock(ContextSnapshot.class);
        given(localSnapshotFactory.captureAll()).willReturn(snapshot);
        given(snapshot.wrap(org.mockito.ArgumentMatchers.any(Runnable.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        ObservationRegistry localRegistry = ObservationRegistry.create();
        WebSocketMessageBrokerConfig config = config(localRegistry, localSnapshotFactory);
        ThreadPoolTaskExecutor executor = config.webSocketOutboundExecutor();
        TaskDecorator decorator = (TaskDecorator) ReflectionTestUtils.getField(executor, "taskDecorator");
        AtomicBoolean executed = new AtomicBoolean();
        Observation parent = Observation.start("parent", localRegistry);

        try (Observation.Scope ignored = parent.openScope()) {
            decorator.decorate(() -> executed.set(true)).run();
        } finally {
            parent.stop();
        }

        assertThat(executed).isTrue();
    }

    private WebSocketMessageBrokerConfig config(
        ObservationRegistry registry,
        ContextSnapshotFactory localSnapshotFactory
    ) {
        return new WebSocketMessageBrokerConfig(
            mock(StompPrincipalInterceptor.class),
            mock(StompAuthChannelInterceptor.class),
            mock(WebSocketRateLimitInterceptor.class),
            mock(WebSocketInboundMetricInterceptor.class),
            mock(WebSocketOutboundMetricInterceptor.class),
            mock(ShutdownAwareHandshakeInterceptor.class),
            mock(ApiResponseStompErrorHandler.class),
            registry,
            localSnapshotFactory,
            mock(WebSocketBrokerProperties.class),
            mock(Environment.class),
            mock(RelayDestinationChannelInterceptors.class)
        );
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
