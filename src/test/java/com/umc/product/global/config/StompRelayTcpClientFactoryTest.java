package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.tcp.TcpOperations;

import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.ssl.SslHandler;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.TcpClient;

@DisplayName("StompRelayTcpClientFactory")
class StompRelayTcpClientFactoryTest {

    @Test
    @DisplayName("TLS relay transport는 secure 연결과 hostname 검증을 활성화한다")
    void configureTlsTransport() {
        WebSocketBrokerProperties.Relay relay = relayProperties(true);

        TcpClient transport = StompRelayTcpClientFactory.configure(TcpClient.create(), relay);

        assertThat(transport.configuration().isSecure()).isTrue();
        SslProvider sslProvider = transport.configuration().sslProvider();
        assertThat(sslProvider).isNotNull();
        SslHandler sslHandler = sslProvider.getSslContext()
            .newHandler(UnpooledByteBufAllocator.DEFAULT, relay.host(), relay.resolvedPort());
        sslProvider.configure(sslHandler);
        assertThat(sslHandler.engine()
            .getSSLParameters()
            .getEndpointIdentificationAlgorithm())
            .isEqualTo("HTTPS");
    }

    @Test
    @DisplayName("TLS를 명시적으로 끈 relay transport만 plaintext를 사용한다")
    void configureExplicitPlaintextTransport() {
        TcpClient transport = StompRelayTcpClientFactory.configure(
            TcpClient.create(),
            relayProperties(false)
        );

        assertThat(transport.configuration().isSecure()).isFalse();
    }

    @Test
    @DisplayName("relay TCP client 문자열은 credential을 노출하지 않는다")
    void tcpClientDescriptionDoesNotExposeCredentials() {
        WebSocketBrokerProperties.Relay relay = relayProperties(true);
        TcpOperations<byte[]> tcpClient = StompRelayTcpClientFactory.create(relay);

        try {
            assertThat(tcpClient.toString()).doesNotContain(
                relay.systemLogin(),
                relay.systemPassword(),
                relay.clientLogin(),
                relay.clientPassword()
            );
        } finally {
            tcpClient.shutdownAsync().join();
        }
    }

    private WebSocketBrokerProperties.Relay relayProperties(boolean tlsEnabled) {
        return new WebSocketBrokerProperties.Relay(
            "broker.internal",
            61614,
            tlsEnabled,
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
