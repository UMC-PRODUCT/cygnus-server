package com.umc.product.global.websocket.handler;

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
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.global.config.WebSocketMessageBrokerConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.websocket.interceptor.ShutdownAwareHandshakeInterceptor;
import com.umc.product.global.websocket.interceptor.StompAuthChannelInterceptor;
import com.umc.product.global.websocket.interceptor.StompPrincipalInterceptor;
import com.umc.product.global.websocket.interceptor.WebSocketInboundMetricInterceptor;
import com.umc.product.global.websocket.interceptor.WebSocketOutboundMetricInterceptor;
import com.umc.product.global.websocket.interceptor.WebSocketRateLimitInterceptor;

import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;

@DisplayName("ApiResponseStompErrorHandler 통합 테스트")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = ApiResponseStompErrorHandlerIntegrationTest.TestApplication.class
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
        SockJsClient sockJsClient = new SockJsClient(
            List.of(new WebSocketTransport(new StandardWebSocketClient()))
        );
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        return stompClient;
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
    @Import({
        WebSocketMessageBrokerConfig.class,
        ApiResponseStompErrorHandler.class,
        WebSocketErrorPublisher.class,
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
    }
}
