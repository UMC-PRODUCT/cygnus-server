package com.umc.product.global.websocket.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.global.config.WebSocketMessageBrokerConfig;
import com.umc.product.global.config.WebSocketSessionExpiryConfig;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.ParsedAccessToken;
import com.umc.product.global.websocket.application.service.StompSubscriptionAuthorizerRegistry;
import com.umc.product.global.websocket.interceptor.ShutdownAwareHandshakeInterceptor;
import com.umc.product.global.websocket.interceptor.StompAuthChannelInterceptor;
import com.umc.product.global.websocket.interceptor.StompPrincipalInterceptor;
import com.umc.product.global.websocket.interceptor.WebSocketInboundMetricInterceptor;
import com.umc.product.global.websocket.interceptor.WebSocketOutboundMetricInterceptor;
import com.umc.product.global.websocket.interceptor.WebSocketRateLimitInterceptor;
import com.umc.product.global.websocket.relay.RelayDestinationChannelInterceptors;
import com.umc.product.global.websocket.relay.RelayDestinationCodec;
import com.umc.product.global.websocket.session.AccessTokenWebSocketSessionRegistry;
import com.umc.product.term.adapter.in.websocket.TermConsentStompInterceptor;
import com.umc.product.term.config.TermConsentEnforcementProperties;
import com.umc.product.term.domain.exception.TermErrorCode;

import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;

@DisplayName("ApiResponseStompErrorHandler 통합 테스트")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = ApiResponseStompErrorHandlerIntegrationTest.TestApplication.class,
    properties = "app.terms.reconsent.enabled=true"
)
@ActiveProfiles("test")
class ApiResponseStompErrorHandlerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("Authorization 헤더 없이 CONNECT하면 ApiResponse 형식의 ERROR 프레임을 받는다")
    void connect_without_authorization_header_receives_api_response_error_frame() throws Exception {
        BlockingQueue<StompErrorFrame> errors = new LinkedBlockingQueue<>();
        WebSocketStompClient stompClient = connect(new StompHeaders(), errors);

        try {
            StompErrorFrame errorFrame = errors.poll(5, TimeUnit.SECONDS);
            assertThat(errorFrame).isNotNull();
            assertErrorFrame(
                errorFrame,
                AuthenticationErrorCode.INVALID_JWT.getCode(),
                AuthenticationErrorCode.INVALID_JWT.getMessage()
            );
        } finally {
            stompClient.stop();
        }
    }

    @Test
    @DisplayName("잘못된 JWT로 CONNECT하면 ApiResponse 형식의 ERROR 프레임을 받는다")
    void connect_with_invalid_jwt_receives_api_response_error_frame() throws Exception {
        when(jwtTokenProvider.parseAndValidateAccessToken(eq("invalid-token")))
            .thenThrow(new AuthenticationDomainException(AuthenticationErrorCode.INVALID_JWT));

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer invalid-token");
        BlockingQueue<StompErrorFrame> errors = new LinkedBlockingQueue<>();
        WebSocketStompClient stompClient = connect(connectHeaders, errors);

        try {
            StompErrorFrame errorFrame = errors.poll(5, TimeUnit.SECONDS);
            assertThat(errorFrame).isNotNull();
            assertErrorFrame(
                errorFrame,
                AuthenticationErrorCode.INVALID_JWT.getCode(),
                AuthenticationErrorCode.INVALID_JWT.getMessage()
            );
        } finally {
            stompClient.stop();
        }
    }

    @Test
    @DisplayName("필수 약관 미동의 JWT로 CONNECT하면 TERMS-0012 ERROR 프레임을 받는다")
    void connectWithRequiredTermReconsentReceivesErrorFrame() throws Exception {
        when(jwtTokenProvider.parseAndValidateAccessToken(eq("reconsent-required-token")))
            .thenReturn(new ParsedAccessToken(
                10L,
                List.of(),
                null,
                null,
                false,
                Instant.now().plusSeconds(3600)
            ));

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer reconsent-required-token");
        BlockingQueue<StompErrorFrame> errors = new LinkedBlockingQueue<>();
        WebSocketStompClient stompClient = connect(connectHeaders, errors);

        try {
            StompErrorFrame errorFrame = errors.poll(5, TimeUnit.SECONDS);
            assertThat(errorFrame).isNotNull();
            assertErrorFrame(
                errorFrame,
                TermErrorCode.TERMS_RECONSENT_REQUIRED.getCode(),
                TermErrorCode.TERMS_RECONSENT_REQUIRED.getMessage()
            );
        } finally {
            stompClient.stop();
        }
    }

    @Test
    @DisplayName("Authorization 헤더 없이 raw STOMP 연결하면 ERROR 프레임을 받는다")
    void stomp_without_authorization_header_receives_error_frame() throws Exception {
        BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        SockJsClient sockJsClient = sockJsClient();
        WebSocketSession session = null;

        try {
            session = sockJsClient.execute(
                new TextWebSocketHandler() {
                    @Override
                    public void afterConnectionEstablished(WebSocketSession establishedSession) throws Exception {
                        establishedSession.sendMessage(
                            new TextMessage("STOMP\naccept-version:1.2\nhost:localhost\n\n\0")
                        );
                    }

                    @Override
                    protected void handleTextMessage(WebSocketSession establishedSession, TextMessage message) {
                        frames.offer(message.getPayload());
                    }
                },
                "http://localhost:%d/ws".formatted(port)
            ).get(5, TimeUnit.SECONDS);

            assertThat(frames.poll(5, TimeUnit.SECONDS))
                .startsWith("ERROR")
                .contains(AuthenticationErrorCode.INVALID_JWT.getCode());
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
            sockJsClient.stop();
        }
    }

    @Test
    @DisplayName("동의 완료 STOMP 연결은 AccessToken 만료 시 실제 WebSocket session을 종료한다")
    void agreedStompConnectionClosesAtAccessTokenExpiry() throws Exception {
        when(jwtTokenProvider.parseAndValidateAccessToken(eq("expiring-agreed-token")))
            .thenReturn(new ParsedAccessToken(
                10L,
                List.of(),
                null,
                null,
                true,
                Instant.now().plusSeconds(3)
            ));

        BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        BlockingQueue<CloseStatus> closeStatuses = new LinkedBlockingQueue<>();
        SockJsClient sockJsClient = sockJsClient();
        WebSocketSession session = null;

        try {
            session = sockJsClient.execute(
                new TextWebSocketHandler() {
                    @Override
                    public void afterConnectionEstablished(WebSocketSession establishedSession) throws Exception {
                        establishedSession.sendMessage(new TextMessage(
                            "CONNECT\nAuthorization:Bearer expiring-agreed-token\n"
                                + "accept-version:1.2\nhost:localhost\n\n\0"
                        ));
                    }

                    @Override
                    protected void handleTextMessage(WebSocketSession establishedSession, TextMessage message) {
                        frames.offer(message.getPayload());
                    }

                    @Override
                    public void afterConnectionClosed(
                        WebSocketSession establishedSession,
                        CloseStatus status
                    ) {
                        closeStatuses.offer(status);
                    }
                },
                "http://localhost:%d/ws".formatted(port)
            ).get(5, TimeUnit.SECONDS);

            assertThat(frames.poll(5, TimeUnit.SECONDS)).startsWith("CONNECTED");
            assertThat(closeStatuses.poll(8, TimeUnit.SECONDS)).isNotNull();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
            sockJsClient.stop();
        }
    }

    @Test
    @DisplayName("server-only MESSAGE 명령을 client inbound로 보내면 ERROR 프레임을 받는다")
    void message_from_client_receives_error_frame() throws Exception {
        when(jwtTokenProvider.parseAndValidateAccessToken(eq("valid-token")))
            .thenReturn(new ParsedAccessToken(10L, List.of(), null));

        BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        SockJsClient sockJsClient = sockJsClient();
        WebSocketSession session = null;

        try {
            session = sockJsClient.execute(
                new TextWebSocketHandler() {
                    @Override
                    public void afterConnectionEstablished(WebSocketSession establishedSession) throws Exception {
                        establishedSession.sendMessage(new TextMessage(
                            "CONNECT\nAuthorization:Bearer valid-token\naccept-version:1.2\nhost:localhost\n\n\0"
                        ));
                    }

                    @Override
                    protected void handleTextMessage(
                        WebSocketSession establishedSession,
                        TextMessage message
                    ) throws Exception {
                        if (message.getPayload().startsWith("CONNECTED")) {
                            establishedSession.sendMessage(new TextMessage(
                                "MESSAGE\ndestination:/app/test/messages\n\nattack\0"
                            ));
                            return;
                        }
                        frames.offer(message.getPayload());
                    }
                },
                "http://localhost:%d/ws".formatted(port)
            ).get(5, TimeUnit.SECONDS);

            assertThat(frames.poll(5, TimeUnit.SECONDS))
                .startsWith("ERROR")
                .contains(CommonErrorCode.SECURITY_WEBSOCKET_INVALID_DESTINATION.getCode());
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
            sockJsClient.stop();
        }
    }

    private WebSocketStompClient connect(StompHeaders connectHeaders, BlockingQueue<StompErrorFrame> errors) {
        WebSocketStompClient stompClient = stompClient();
        stompClient.connectAsync(
            "http://localhost:%d/ws".formatted(port),
            new WebSocketHttpHeaders(),
            connectHeaders,
            new ErrorCollectingSessionHandler(errors)
        );
        return stompClient;
    }

    private WebSocketStompClient stompClient() {
        return new WebSocketStompClient(sockJsClient());
    }

    private SockJsClient sockJsClient() {
        return new SockJsClient(
            List.of(new WebSocketTransport(new StandardWebSocketClient()))
        );
    }

    private void assertErrorFrame(StompErrorFrame errorFrame, String code, String message) throws Exception {
        assertThat(errorFrame.headers().getContentType()).isEqualTo(MimeTypeUtils.APPLICATION_JSON);

        assertApiResponsePayload(errorFrame.payload(), code, message);
    }

    private void assertApiResponsePayload(String payload, String code, String message) throws Exception {
        JsonNode body = objectMapper.readTree(payload);
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("code").asText()).isEqualTo(code);
        assertThat(body.path("message").asText()).isEqualTo(message);
        assertThat(body.has("result")).isFalse();
    }

    private record StompErrorFrame(StompHeaders headers, String payload) {
    }

    private static class ErrorCollectingSessionHandler extends StompSessionHandlerAdapter {

        private final BlockingQueue<StompErrorFrame> errors;

        private ErrorCollectingSessionHandler(BlockingQueue<StompErrorFrame> errors) {
            this.errors = errors;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return byte[].class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            byte[] bytes = (byte[]) payload;
            errors.offer(new StompErrorFrame(headers, new String(bytes, StandardCharsets.UTF_8)));
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            session.disconnect();
        }
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
    @EnableConfigurationProperties(TermConsentEnforcementProperties.class)
    @Import({
        WebSocketMessageBrokerConfig.class,
        WebSocketSessionExpiryConfig.class,
        ApiResponseStompErrorHandler.class,
        WebSocketErrorPublisher.class,
        StompSubscriptionAuthorizerRegistry.class,
        StompPrincipalInterceptor.class,
        StompAuthChannelInterceptor.class,
        WebSocketRateLimitInterceptor.class,
        WebSocketInboundMetricInterceptor.class,
        WebSocketOutboundMetricInterceptor.class,
        ShutdownAwareHandshakeInterceptor.class,
        RelayDestinationChannelInterceptors.class,
        RelayDestinationCodec.class,
        AccessTokenWebSocketSessionRegistry.class,
        TermConsentStompInterceptor.class
    })
    static class TestApplication {

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        OperationalMetrics operationalMetrics(MeterRegistry meterRegistry) {
            return new OperationalMetrics(meterRegistry);
        }

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }

        @Bean
        ObservationRegistry observationRegistry() {
            return ObservationRegistry.create();
        }

        @Bean
        ContextSnapshotFactory contextSnapshotFactory() {
            return ContextSnapshotFactory.builder().build();
        }
    }
}
