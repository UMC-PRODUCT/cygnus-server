package com.umc.product.global.config;

import com.umc.product.global.websocket.handler.ApiResponseStompErrorHandler;
import com.umc.product.global.websocket.interceptor.ShutdownAwareHandshakeInterceptor;
import com.umc.product.global.websocket.interceptor.StompAuthChannelInterceptor;
import com.umc.product.global.websocket.interceptor.StompPrincipalInterceptor;
import com.umc.product.global.websocket.interceptor.WebSocketInboundMetricInterceptor;
import com.umc.product.global.websocket.interceptor.WebSocketOutboundMetricInterceptor;
import com.umc.product.global.websocket.interceptor.WebSocketRateLimitInterceptor;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketMessageBrokerConfig implements WebSocketMessageBrokerConfigurer {

    private final StompPrincipalInterceptor stompPrincipalInterceptor;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final WebSocketRateLimitInterceptor webSocketRateLimitInterceptor;
    private final WebSocketInboundMetricInterceptor webSocketInboundMetricInterceptor;
    private final WebSocketOutboundMetricInterceptor webSocketOutboundMetricInterceptor;
    private final ShutdownAwareHandshakeInterceptor shutdownAwareHandshakeInterceptor;
    private final ApiResponseStompErrorHandler apiResponseStompErrorHandler;
    private final ObservationRegistry observationRegistry;
    private final ContextSnapshotFactory snapshotFactory;

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
        registry.enableSimpleBroker("/topic", "/queue")
            .setHeartbeatValue(new long[]{4000, 4000})
            .setTaskScheduler(webSocketHeartbeatScheduler());
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
            stompPrincipalInterceptor,
            stompAuthChannelInterceptor,
            webSocketInboundMetricInterceptor,
            webSocketRateLimitInterceptor
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
        registration.interceptors(webSocketOutboundMetricInterceptor);
        registration.taskExecutor(webSocketOutboundExecutor());
    }
}
