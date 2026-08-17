package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

@DisplayName("WebSocket broker relay port 정책")
class WebSocketBrokerPortPolicyTest {

    private static final int LOCAL_DEFAULT_PORT = 61613;
    private static final int EXPLICIT_RELAY_PORT = 61614;
    private static final String SYSTEM_PASSWORD = "system-port-secret";
    private static final String CLIENT_PASSWORD = "client-port-secret";

    @Test
    @DisplayName("빈 relay port binding은 외부 미공급 상태와 local 기본값을 구분한다")
    void bindEmptyPortPreservesMissingValue() {
        WebSocketBrokerProperties properties = bindRelayPort("");

        assertThat(properties.relay().port()).isNull();
        assertThat(properties.relay().resolvedPort()).isEqualTo(LOCAL_DEFAULT_PORT);
    }

    @Test
    @DisplayName("명시적으로 공급한 relay port binding은 local 기본값보다 우선한다")
    void bindExplicitPortPreservesValue() {
        WebSocketBrokerProperties properties = bindRelayPort(String.valueOf(EXPLICIT_RELAY_PORT));

        assertThat(properties.relay().port()).isEqualTo(EXPLICIT_RELAY_PORT);
        assertThat(properties.relay().resolvedPort()).isEqualTo(EXPLICIT_RELAY_PORT);
    }

    @Test
    @DisplayName("application.yml은 relay port를 외부 공급 없이는 기본 설정하지 않는다")
    void applicationYamlDoesNotSupplyRelayPortDefault() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));

        Properties properties = yaml.getObject();

        assertThat(properties)
            .containsEntry(
                "app.websocket.broker.relay.port",
                "${WEBSOCKET_BROKER_RELAY_PORT:}"
            );
    }

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

    private WebSocketBrokerProperties bindRelayPort(String port) {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
            "app.websocket.broker.mode", "relay",
            "app.websocket.broker.relay.port", port
        ));
        return new Binder(source)
            .bind("app.websocket.broker", Bindable.of(WebSocketBrokerProperties.class))
            .orElseThrow(() -> new IllegalStateException("broker properties binding failed"));
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
