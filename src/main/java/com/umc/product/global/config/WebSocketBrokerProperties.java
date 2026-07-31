package com.umc.product.global.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.websocket.broker")
public record WebSocketBrokerProperties(
    Mode mode,
    Relay relay
) {

    public WebSocketBrokerProperties {
        mode = mode == null ? Mode.SIMPLE : mode;
        relay = relay == null ? Relay.defaults() : relay;
    }

    public enum Mode {
        SIMPLE,
        RELAY
    }

    public record Relay(
        String host,
        Integer port,
        Boolean tlsEnabled,
        String virtualHost,
        String systemLogin,
        String systemPassword,
        String clientLogin,
        String clientPassword,
        Duration systemHeartbeatSendInterval,
        Duration systemHeartbeatReceiveInterval,
        Duration startupTimeout
    ) {

        private static final int DEFAULT_PORT = 61613;
        private static final Duration DEFAULT_HEARTBEAT = Duration.ofSeconds(10);
        private static final Duration DEFAULT_STARTUP_TIMEOUT = Duration.ofSeconds(5);

        public Relay {
            tlsEnabled = tlsEnabled == null ? Boolean.TRUE : tlsEnabled;
            systemHeartbeatSendInterval = systemHeartbeatSendInterval == null
                ? DEFAULT_HEARTBEAT
                : systemHeartbeatSendInterval;
            systemHeartbeatReceiveInterval = systemHeartbeatReceiveInterval == null
                ? DEFAULT_HEARTBEAT
                : systemHeartbeatReceiveInterval;
            startupTimeout = startupTimeout == null ? DEFAULT_STARTUP_TIMEOUT : startupTimeout;
        }

        static Relay defaults() {
            return new Relay(
                null,
                null,
                Boolean.TRUE,
                null,
                null,
                null,
                null,
                null,
                DEFAULT_HEARTBEAT,
                DEFAULT_HEARTBEAT,
                DEFAULT_STARTUP_TIMEOUT
            );
        }

        public int resolvedPort() {
            return port == null ? DEFAULT_PORT : port;
        }

        @Override
        public String toString() {
            return "Relay[port=" + port
                + ", tlsEnabled=" + tlsEnabled
                + ", systemHeartbeatSendInterval=" + systemHeartbeatSendInterval
                + ", systemHeartbeatReceiveInterval=" + systemHeartbeatReceiveInterval
                + ", startupTimeout=" + startupTimeout
                + ", credentials=<redacted>]";
        }
    }
}
