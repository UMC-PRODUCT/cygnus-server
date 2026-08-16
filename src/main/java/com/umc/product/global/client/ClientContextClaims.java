package com.umc.product.global.client;

public record ClientContextClaims(
    String clientId,
    ClientServiceType serviceType,
    ClientEnvironment environment
) {
    public ClientContextClaims {
        serviceType = serviceType == null ? ClientServiceType.UNKNOWN : serviceType;
        environment = environment == null ? ClientEnvironment.UNKNOWN : environment;
    }

    public static ClientContextClaims empty() {
        return new ClientContextClaims(null, ClientServiceType.UNKNOWN, ClientEnvironment.UNKNOWN);
    }

    public static ClientContextClaims of(
        String clientId,
        ClientServiceType serviceType,
        ClientEnvironment environment
    ) {
        return new ClientContextClaims(
            clientId,
            serviceType,
            environment
        );
    }
}
