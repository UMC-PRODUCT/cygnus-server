package com.umc.product.global.websocket.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration;
import org.springframework.boot.autoconfigure.graphql.servlet.GraphQlWebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import com.umc.product.global.config.WebSocketMessageBrokerConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.ParsedAccessToken;
import com.umc.product.global.websocket.adapter.out.StompBroadcastAdapter;
import com.umc.product.global.websocket.application.port.in.StompSubscriptionAuthorizer;
import com.umc.product.global.websocket.application.port.out.BroadcastPort;
import com.umc.product.global.websocket.application.service.StompSubscriptionAuthorizerRegistry;
import com.umc.product.global.websocket.handler.ApiResponseStompErrorHandler;
import com.umc.product.global.websocket.handler.WebSocketErrorPublisher;

import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;

@DisplayName("STOMP 소비 도메인 구독 인가 통합 테스트")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = StompSubscriptionAuthorizationIntegrationTest.TestApplication.class
)
@ActiveProfiles("test")
class StompSubscriptionAuthorizationIntegrationTest {

    private static final String DESTINATION = "/topic/test/rooms/10/messages";

    @LocalServerPort
    private int port;

    @Autowired
    private BroadcastPort broadcastPort;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("소비 도메인 authorizer가 승인한 topic을 구독하고 broadcast를 수신한다")
    void authorizedSubscriptionReceivesBroadcast() throws Exception {
        when(jwtTokenProvider.parseAndValidateAccessToken(eq("valid-token")))
            .thenReturn(new ParsedAccessToken(10L, List.of(), null));

        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        WebSocketStompClient stompClient = stompClient();
        StompSession session = null;

        try {
            session = connect(stompClient);
            session.subscribe(DESTINATION, frameHandler(messages));

            String receivedMessage = awaitBroadcast(messages);

            assertThat(receivedMessage)
                .contains("\"id\":100")
                .contains("\"content\":\"안녕하세요\"");
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
            stompClient.stop();
        }
    }

    private String awaitBroadcast(BlockingQueue<String> messages) throws InterruptedException {
        for (int attempt = 0; attempt < 20; attempt++) {
            broadcastPort.broadcast(DESTINATION, new TestPayload(100L, "안녕하세요"));
            String message = messages.poll(250, TimeUnit.MILLISECONDS);
            if (message != null) {
                return message;
            }
        }
        return null;
    }

    private StompSession connect(WebSocketStompClient stompClient) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer valid-token");
        return stompClient.connectAsync(
            "http://localhost:%d/ws".formatted(port),
            new WebSocketHttpHeaders(),
            connectHeaders,
            new StompSessionHandlerAdapter() {
            }
        ).get(5, TimeUnit.SECONDS);
    }

    private WebSocketStompClient stompClient() {
        SockJsClient sockJsClient = new SockJsClient(
            List.of(new WebSocketTransport(new StandardWebSocketClient()))
        );
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        return stompClient;
    }

    private StompFrameHandler frameHandler(BlockingQueue<String> messages) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                byte[] bytes = (byte[]) payload;
                messages.offer(new String(bytes, StandardCharsets.UTF_8));
            }
        };
    }

    private record TestPayload(Long id, String content) {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        GraphQlAutoConfiguration.class,
        GraphQlWebMvcAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ManagementWebSecurityAutoConfiguration.class
    })
    @Import({
        WebSocketMessageBrokerConfig.class,
        ApiResponseStompErrorHandler.class,
        WebSocketErrorPublisher.class,
        StompBroadcastAdapter.class,
        StompSubscriptionAuthorizerRegistry.class,
        StompPrincipalInterceptor.class,
        StompAuthChannelInterceptor.class,
        WebSocketRateLimitInterceptor.class,
        WebSocketInboundMetricInterceptor.class,
        WebSocketOutboundMetricInterceptor.class,
        ShutdownAwareHandshakeInterceptor.class
    })
    static class TestApplication {

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        ObservationRegistry observationRegistry() {
            return ObservationRegistry.create();
        }

        @Bean
        ContextSnapshotFactory contextSnapshotFactory() {
            return ContextSnapshotFactory.builder().build();
        }

        @Bean
        StompSubscriptionAuthorizer testSubscriptionAuthorizer() {
            return new StompSubscriptionAuthorizer() {
                @Override
                public boolean supports(String destination) {
                    return DESTINATION.equals(destination);
                }

                @Override
                public boolean isAuthorized(Long memberId, String destination) {
                    return memberId.equals(10L);
                }
            };
        }
    }
}
