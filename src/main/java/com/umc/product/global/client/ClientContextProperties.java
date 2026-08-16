package com.umc.product.global.client;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.client-context")
public record ClientContextProperties(
    List<Origin> origins
) {

    public ClientContextProperties {
        origins = origins == null ? List.of() : List.copyOf(origins);
    }

    public record Origin(
        String origin,
        ClientServiceType serviceType,
        ClientEnvironment environment
    ) {

        public Origin {
            serviceType = serviceType == null ? ClientServiceType.UNKNOWN : serviceType;
            environment = environment == null ? ClientEnvironment.UNKNOWN : environment;
        }
    }
}
