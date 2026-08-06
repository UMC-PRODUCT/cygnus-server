package com.umc.product.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.SimpleBrokerRegistration;
import org.springframework.messaging.simp.config.StompBrokerRelayRegistration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

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
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(WebSocketBrokerProperties.class)
@Import(StompSendAuthorizationConfig.class)
@RequiredArgsConstructor
public class WebSocketMessageBrokerConfig implements WebSocketMessageBrokerConfigurer {

    private static final String USER_DESTINATION_BROADCAST = "/topic/__internal.user-destination";
    private static final String USER_REGISTRY_BROADCAST = "/topic/__internal.user-registry";

    private final StompPrincipalInterceptor stompPrincipalInterceptor;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final WebSocketRateLimitInterceptor webSocketRateLimitInterceptor;
    private final WebSocketInboundMetricInterceptor webSocketInboundMetricInterceptor;
    private final WebSocketOutboundMetricInterceptor webSocketOutboundMetricInterceptor;
    private final ShutdownAwareHandshakeInterceptor shutdownAwareHandshakeInterceptor;
    private final ApiResponseStompErrorHandler apiResponseStompErrorHandler;
    private final ObservationRegistry observationRegistry;
    private final ContextSnapshotFactory snapshotFactory;
    private final WebSocketBrokerProperties brokerProperties;
    private final Environment environment;
    private final RelayDestinationChannelInterceptors relayDestinationChannelInterceptors;

    @Bean
    public ThreadPoolTaskScheduler webSocketHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        return scheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.setErrorHandler(apiResponseStompErrorHandler);
        registry.addEndpoint("/ws")
            .addInterceptors(shutdownAwareHandshakeInterceptor)
            .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        WebSocketBrokerPropertiesValidator.validate(brokerProperties, environment);
        ThreadPoolTaskScheduler heartbeatScheduler = webSocketHeartbeatScheduler();
        registry.configureBrokerChannel()
            .interceptors(relayDestinationChannelInterceptors.toBroker());

        switch (brokerProperties.mode()) {
            case SIMPLE -> configureSimpleBroker(registry, heartbeatScheduler);
            case RELAY -> configureRelayBroker(registry, heartbeatScheduler, brokerProperties.relay());
        }
        registry.setApplicationDestinationPrefixes("/app");
    }

    private void configureSimpleBroker(
        MessageBrokerRegistry registry,
        ThreadPoolTaskScheduler heartbeatScheduler
    ) {
        SimpleBrokerRegistration registration = registry.enableSimpleBroker("/topic", "/queue");
        registration.setHeartbeatValue(new long[]{4000, 4000});
        registration.setTaskScheduler(heartbeatScheduler);
    }

    private void configureRelayBroker(
        MessageBrokerRegistry registry,
        ThreadPoolTaskScheduler heartbeatScheduler,
        WebSocketBrokerProperties.Relay relay
    ) {
        StompBrokerRelayRegistration registration = registry.enableStompBrokerRelay("/topic", "/queue");
        registration.setRelayHost(relay.host());
        registration.setRelayPort(relay.resolvedPort());
        registration.setVirtualHost(relay.virtualHost());
        registration.setSystemLogin(relay.systemLogin());
        registration.setSystemPasscode(relay.systemPassword());
        registration.setClientLogin(relay.clientLogin());
        registration.setClientPasscode(relay.clientPassword());
        registration.setTcpClient(StompRelayTcpClientFactory.create(relay));
        registration.setSystemHeartbeatSendInterval(relay.systemHeartbeatSendInterval().toMillis());
        registration.setSystemHeartbeatReceiveInterval(relay.systemHeartbeatReceiveInterval().toMillis());
        registration.setTaskScheduler(heartbeatScheduler);
        registration.setUserDestinationBroadcast(USER_DESTINATION_BROADCAST);
        registration.setUserRegistryBroadcast(USER_REGISTRY_BROADCAST);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
            stompPrincipalInterceptor,
            webSocketRateLimitInterceptor,
            stompAuthChannelInterceptor,
            webSocketInboundMetricInterceptor,
            relayDestinationChannelInterceptors.toBroker()
        );
        registration.taskExecutor()
            .corePoolSize(32)
            .maxPoolSize(32)
            .queueCapacity(2048);
    }

    @Bean
    public ThreadPoolTaskExecutor webSocketOutboundExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(16);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(4096);
        executor.setThreadNamePrefix("ws-outbound-");
        executor.setTaskDecorator(runnable -> {
            ContextSnapshot snapshot = snapshotFactory.captureAll();
            return snapshot.wrap(() -> {
                Observation parent = observationRegistry.getCurrentObservation();
                if (parent != null) {
                    Observation.createNotStarted("websocket.outbound", observationRegistry)
                        .parentObservation(parent)
                        .lowCardinalityKeyValue("thread", Thread.currentThread().getName())
                        .observe(runnable);
                } else {
                    runnable.run();
                }
            });
        });
        return executor;
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(
            relayDestinationChannelInterceptors.fromBroker(),
            webSocketOutboundMetricInterceptor
        );
        registration.taskExecutor(webSocketOutboundExecutor());
    }
}
