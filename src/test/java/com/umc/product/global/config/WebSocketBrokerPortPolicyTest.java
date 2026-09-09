package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

@DisplayName("WebSocket broker relay port 정책")
class WebSocketBrokerPortPolicyTest {

    private static final int LOCAL_DEFAULT_PORT = 61613;
    private static final String SYSTEM_PASSWORD = "system-port-secret";
    private static final String CLIENT_PASSWORD = "client-port-secret";

    @Test
    @DisplayName(
        "alpha 또는 prod가 포함된 프로필은 relay port 미공급을 credential 노출 없이 거부한다"
    )
    void sharedProfilesRejectMissingPortWithoutExposingCredentials() {
        WebSocketBrokerProperties properties = relayProperties(null);

        for (String[] profiles : new String[][] {{"test", "alpha"}, {"local", "prod"}}) {
            MockEnvironment environment = new MockEnvironment();
            environment.setActiveProfiles(profiles);

            assertThatThrownBy(() -> WebSocketBrokerPropertiesValidator.validate(properties, environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("port")
                .hasMessageNotContaining(SYSTEM_PASSWORD)
                .hasMessageNotContaining(CLIENT_PASSWORD);
        }
    }

    @Test
    @DisplayName("local과 test 프로필은 relay port 미공급 시 기본 port를 사용할 수 있다")
    void localAndTestProfilesAllowMissingPortFallback() {
        WebSocketBrokerProperties properties = relayProperties(null);

        for (String profile : new String[] {"local", "test"}) {
            MockEnvironment environment = new MockEnvironment();
            environment.setActiveProfiles(profile);

            assertThatCode(() -> WebSocketBrokerPropertiesValidator.validate(properties, environment))
                .doesNotThrowAnyException();
        }
        assertThat(properties.relay().resolvedPort()).isEqualTo(LOCAL_DEFAULT_PORT);
    }

    private WebSocketBrokerProperties relayProperties(Integer port) {
        return new WebSocketBrokerProperties(
            WebSocketBrokerProperties.Mode.RELAY,
            new WebSocketBrokerProperties.Relay(
                "broker.internal",
                port,
                true,
                "/product",
                "system-user",
                SYSTEM_PASSWORD,
                "client-user",
                CLIENT_PASSWORD,
                Duration.ofSeconds(10),
                Duration.ofSeconds(10),
                Duration.ofSeconds(5)
            )
        );
    }
}
