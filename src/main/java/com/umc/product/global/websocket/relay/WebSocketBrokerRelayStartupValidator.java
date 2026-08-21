package com.umc.product.global.websocket.relay;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import com.umc.product.global.config.WebSocketBrokerProperties;

@Component
@ConditionalOnProperty(prefix = "app.websocket.broker", name = "mode", havingValue = "relay")
public class WebSocketBrokerRelayStartupValidator implements ApplicationRunner {

    private static final Profiles SHARED_ENVIRONMENT_PROFILES = Profiles.of("alpha", "prod");

    private final WebSocketBrokerProperties properties;
    private final Environment environment;
    private final StompBrokerRelayMonitor relayMonitor;

    public WebSocketBrokerRelayStartupValidator(
        WebSocketBrokerProperties properties,
        Environment environment,
        StompBrokerRelayMonitor relayMonitor
    ) {
        this.properties = properties;
        this.environment = environment;
        this.relayMonitor = relayMonitor;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!environment.acceptsProfiles(SHARED_ENVIRONMENT_PROFILES)) {
            return;
        }

        try {
            if (!relayMonitor.awaitAvailable(properties.relay().startupTimeout())) {
                throw new ApplicationContextException(
                    "STOMP broker relay가 startup-timeout 안에 준비되지 않았습니다."
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApplicationContextException(
                "STOMP broker relay startup readiness 대기가 중단되었습니다.",
                exception
            );
        }
    }
}
