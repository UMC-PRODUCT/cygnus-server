package com.umc.product.global.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

public final class WebSocketBrokerPropertiesValidator {

    private static final Profiles SHARED_ENVIRONMENT_PROFILES = Profiles.of("alpha", "prod");

    private WebSocketBrokerPropertiesValidator() {
    }

    public static void validate(WebSocketBrokerProperties properties, Environment environment) {
        if (properties.mode() == WebSocketBrokerProperties.Mode.SIMPLE) {
            return;
        }

        boolean sharedEnvironment = environment.acceptsProfiles(SHARED_ENVIRONMENT_PROFILES);
        validateRelay(properties.relay(), sharedEnvironment);
    }

    private static void validateRelay(WebSocketBrokerProperties.Relay relay, boolean sharedEnvironment) {
        List<String> missingProperties = new ArrayList<>();
        requireText(missingProperties, relay.host(), "host");
        requireText(missingProperties, relay.virtualHost(), "virtual-host");
        requireText(missingProperties, relay.systemLogin(), "system-login");
        requireText(missingProperties, relay.systemPassword(), "system-password");
        requireText(missingProperties, relay.clientLogin(), "client-login");
        requireText(missingProperties, relay.clientPassword(), "client-password");
        if (sharedEnvironment && relay.port() == null) {
            missingProperties.add("port");
        }

        if (!missingProperties.isEmpty()) {
            throw new IllegalStateException(
                "app.websocket.broker.relay 필수 설정이 누락되었습니다: "
                    + String.join(", ", missingProperties)
            );
        }
        if (relay.resolvedPort() < 1 || relay.resolvedPort() > 65_535) {
            throw new IllegalStateException("app.websocket.broker.relay.port는 1..65535 범위여야 합니다.");
        }
        if (sharedEnvironment && !relay.tlsEnabled()) {
            throw new IllegalStateException(
                "dev/prod 프로필에서는 app.websocket.broker.relay.tls-enabled=true가 필요합니다."
            );
        }
        requireNonNegative(relay.systemHeartbeatSendInterval(), "system-heartbeat-send-interval");
        requireNonNegative(relay.systemHeartbeatReceiveInterval(), "system-heartbeat-receive-interval");
        if (relay.startupTimeout().isZero() || relay.startupTimeout().isNegative()) {
            throw new IllegalStateException(
                "app.websocket.broker.relay.startup-timeout은 양수여야 합니다."
            );
        }
    }

    private static void requireText(List<String> missingProperties, String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            missingProperties.add(propertyName);
        }
    }

    private static void requireNonNegative(Duration value, String propertyName) {
        if (value.isNegative()) {
            throw new IllegalStateException(
                "app.websocket.broker.relay." + propertyName + "은 음수일 수 없습니다."
            );
        }
    }
}
