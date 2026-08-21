package com.umc.product.global.websocket.relay;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ApplicationContextException;
import org.springframework.mock.env.MockEnvironment;

import com.umc.product.global.config.WebSocketBrokerProperties;

@DisplayName("WebSocketBrokerRelayStartupValidator")
class WebSocketBrokerRelayStartupValidatorTest {

    private final StompBrokerRelayMonitor monitor = org.mockito.Mockito.mock(StompBrokerRelayMonitor.class);

    @Test
    @DisplayName("prod에서 relay가 제한 시간 안에 연결되지 않으면 시작을 거부한다")
    void productionRequiresAvailableRelay() throws InterruptedException {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        WebSocketBrokerProperties properties = relayProperties();
        given(monitor.awaitAvailable(properties.relay().startupTimeout())).willReturn(false);
        WebSocketBrokerRelayStartupValidator sut = new WebSocketBrokerRelayStartupValidator(
            properties,
            environment,
            monitor
        );

        assertThatThrownBy(() -> sut.run(new DefaultApplicationArguments(new String[0])))
            .isInstanceOf(ApplicationContextException.class)
            .hasMessageContaining("STOMP broker relay");
    }

    @Test
    @DisplayName("local에서는 relay startup readiness를 강제하지 않는다")
    void localDoesNotRequireStartupReadiness() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        WebSocketBrokerRelayStartupValidator sut = new WebSocketBrokerRelayStartupValidator(
            relayProperties(),
            environment,
            monitor
        );

        assertThatCode(() -> sut.run(new DefaultApplicationArguments(new String[0])))
            .doesNotThrowAnyException();
        verifyNoInteractions(monitor);
    }

    @Test
    @DisplayName("alpha 또는 prod가 포함된 혼합 프로필은 relay startup readiness를 강제한다")
    void mixedSharedProfilesRequireStartupReadiness() throws InterruptedException {
        WebSocketBrokerProperties properties = relayProperties();
        given(monitor.awaitAvailable(properties.relay().startupTimeout())).willReturn(false);

        for (String[] profiles : new String[][] {{"test", "alpha"}, {"local", "prod"}}) {
            MockEnvironment environment = new MockEnvironment();
            environment.setActiveProfiles(profiles);
            WebSocketBrokerRelayStartupValidator sut = new WebSocketBrokerRelayStartupValidator(
                properties,
                environment,
                monitor
            );

            assertThatThrownBy(() -> sut.run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(ApplicationContextException.class)
                .hasMessageContaining("STOMP broker relay");
        }
        verify(monitor, times(2)).awaitAvailable(properties.relay().startupTimeout());
    }

    private WebSocketBrokerProperties relayProperties() {
        return new WebSocketBrokerProperties(
            WebSocketBrokerProperties.Mode.RELAY,
            new WebSocketBrokerProperties.Relay(
                "broker.internal",
                61613,
                true,
                "/product",
                "system-user",
                "system-password",
                "client-user",
                "client-password",
                Duration.ofSeconds(10),
                Duration.ofSeconds(10),
                Duration.ofMillis(50)
            )
        );
    }
}
