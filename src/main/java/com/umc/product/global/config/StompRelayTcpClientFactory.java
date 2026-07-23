package com.umc.product.global.config;

import javax.net.ssl.SSLParameters;

import org.springframework.messaging.simp.stomp.StompReactorNettyCodec;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;

import io.netty.handler.ssl.SslHandler;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpSslContextSpec;

final class StompRelayTcpClientFactory {

    private static final String HOSTNAME_VERIFICATION_ALGORITHM = "HTTPS";

    private StompRelayTcpClientFactory() {
    }

    static TcpOperations<byte[]> create(WebSocketBrokerProperties.Relay relay) {
        return new ReactorNettyTcpClient<>(
            tcpClient -> configure(tcpClient, relay),
            new StompReactorNettyCodec()
        );
    }

    static TcpClient configure(TcpClient tcpClient, WebSocketBrokerProperties.Relay relay) {
        TcpClient configured = tcpClient.host(relay.host()).port(relay.resolvedPort());
        if (!relay.tlsEnabled()) {
            return configured;
        }

        TcpSslContextSpec sslContext = TcpSslContextSpec.forClient();
        return configured.secure(ssl -> ssl
            .sslContext((SslProvider.GenericSslContextSpec<?>) sslContext)
            .handlerConfigurator(StompRelayTcpClientFactory::enableHostnameVerification));
    }

    private static void enableHostnameVerification(SslHandler handler) {
        SSLParameters sslParameters = handler.engine().getSSLParameters();
        sslParameters.setEndpointIdentificationAlgorithm(HOSTNAME_VERIFICATION_ALGORITHM);
        handler.engine().setSSLParameters(sslParameters);
    }
}
