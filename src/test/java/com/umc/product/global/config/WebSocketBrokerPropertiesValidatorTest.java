package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

@DisplayName("WebSocket broker 설정 검증")
class WebSocketBrokerPropertiesValidatorTest {

    @Test
    @DisplayName("local 프로필은 simple broker를 사용할 수 있다")
    void validate_localSimpleBroker() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        assertThatCode(() -> WebSocketBrokerPropertiesValidator.validate(
            new WebSocketBrokerProperties(WebSocketBrokerProperties.Mode.SIMPLE, null),
            environment
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("alpha와 prod 프로필도 single instance에서는 simple broker로 시작할 수 있다")
    void validate_sharedEnvironmentAllowsSimpleBroker() {
        for (String profile : new String[] {"alpha", "prod"}) {
            MockEnvironment environment = new MockEnvironment();
            environment.setActiveProfiles(profile);

            assertThatCode(() -> WebSocketBrokerPropertiesValidator.validate(
                new WebSocketBrokerProperties(WebSocketBrokerProperties.Mode.SIMPLE, null),
                environment
            )).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("alpha 또는 prod가 포함된 혼합 프로필도 simple broker로 시작할 수 있다")
    void validate_mixedSharedProfilesAllowsSimpleBroker() {
        for (String[] profiles : new String[][] {{"test", "alpha"}, {"local", "prod"}}) {
            MockEnvironment environment = new MockEnvironment();
            environment.setActiveProfiles(profiles);

            assertThatCode(() -> WebSocketBrokerPropertiesValidator.validate(
                new WebSocketBrokerProperties(WebSocketBrokerProperties.Mode.SIMPLE, null),
                environment
            )).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("relay TLS는 설정을 생략해도 기본 활성화된다")
    void relayTlsDefaultsToEnabled() {
        WebSocketBrokerProperties.Relay relay = new WebSocketBrokerProperties.Relay(
            "broker.internal",
            61614,
            null,
            "/product",
            "system-user",
            "system-password",
            "client-user",
            "client-password",
            Duration.ofSeconds(10),
            Duration.ofSeconds(10),
            Duration.ofSeconds(5)
        );

        assertThat(relay.tlsEnabled()).isTrue();
    }

    @Test
    @DisplayName("local 또는 test에서만 relay plaintext를 명시적으로 허용한다")
    void validate_nonSharedProfileAllowsExplicitPlaintextRelay() {
        for (String profile : new String[] {"local", "test"}) {
            MockEnvironment environment = new MockEnvironment();
            environment.setActiveProfiles(profile);

            assertThatCode(() -> WebSocketBrokerPropertiesValidator.validate(
                relayProperties(false),
                environment
            )).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("alpha 또는 prod는 relay plaintext를 credential 노출 없이 거부한다")
    void validate_sharedProfileRejectsPlaintextWithoutExposingCredentials() {
        String systemPassword = "system-plaintext-secret";
        String clientPassword = "client-plaintext-secret";
        WebSocketBrokerProperties properties = relayProperties(
            false,
            new RelayCredentials("system-user", systemPassword, "client-user", clientPassword)
        );

        for (String[] profiles : new String[][] {{"alpha"}, {"test", "prod"}}) {
            MockEnvironment environment = new MockEnvironment();
            environment.setActiveProfiles(profiles);

            assertThatThrownBy(() -> WebSocketBrokerPropertiesValidator.validate(properties, environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tls-enabled=true")
                .hasMessageNotContaining(systemPassword)
                .hasMessageNotContaining(clientPassword);
        }
    }

    @Test
    @DisplayName("relay credential이 하나라도 비어 있으면 값 노출 없이 시작을 거부한다")
    void validate_missingRelayCredentialDoesNotExposeSecrets() {
        String systemPassword = "system-secret-value";
        String clientPassword = "client-secret-value";
        WebSocketBrokerProperties properties = new WebSocketBrokerProperties(
            WebSocketBrokerProperties.Mode.RELAY,
            new WebSocketBrokerProperties.Relay(
                "broker.internal",
                61613,
                true,
                "",
                "system-user",
                systemPassword,
                "client-user",
                clientPassword,
                Duration.ofSeconds(10),
                Duration.ofSeconds(10),
                Duration.ofSeconds(5)
            )
        );

        assertThatThrownBy(() -> WebSocketBrokerPropertiesValidator.validate(
            properties,
            new MockEnvironment()
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("virtual-host")
            .hasMessageNotContaining(systemPassword)
            .hasMessageNotContaining(clientPassword);
    }

    @Test
    @DisplayName("relay 설정 문자열은 credential을 노출하지 않는다")
    void relayToStringRedactsCredentials() {
        String systemLogin = "system-user";
        String systemPassword = "system-secret-value";
        String clientLogin = "client-user";
        String clientPassword = "client-secret-value";
        WebSocketBrokerProperties properties = relayProperties(
            true,
            new RelayCredentials(systemLogin, systemPassword, clientLogin, clientPassword)
        );

        assertThatCode(() -> WebSocketBrokerPropertiesValidator.validate(
            properties,
            new MockEnvironment()
        )).doesNotThrowAnyException();

        String description = properties.toString();
        assertThat(description)
            .doesNotContain(systemLogin, systemPassword, clientLogin, clientPassword);
    }

    private WebSocketBrokerProperties relayProperties(boolean tlsEnabled) {
        return relayProperties(
            tlsEnabled,
            new RelayCredentials("system-user", "system-password", "client-user", "client-password")
        );
    }

    private WebSocketBrokerProperties relayProperties(
        boolean tlsEnabled,
        RelayCredentials credentials
    ) {
        return new WebSocketBrokerProperties(
            WebSocketBrokerProperties.Mode.RELAY,
            new WebSocketBrokerProperties.Relay(
                "broker.internal",
                61613,
                tlsEnabled,
                "/product",
                credentials.systemLogin(),
                credentials.systemPassword(),
                credentials.clientLogin(),
                credentials.clientPassword(),
                Duration.ofSeconds(10),
                Duration.ofSeconds(10),
                Duration.ofSeconds(5)
            )
        );
    }

    private record RelayCredentials(
        String systemLogin,
        String systemPassword,
        String clientLogin,
        String clientPassword
    ) {
    }
}
