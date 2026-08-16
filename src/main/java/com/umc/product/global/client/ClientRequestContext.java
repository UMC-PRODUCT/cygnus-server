package com.umc.product.global.client;

public record ClientRequestContext(
    ClientServiceType serviceType,
    ClientDeviceType deviceType,
    ClientEnvironment environment,
    String source,
    boolean mismatched
) {

    public ClientRequestContext {
        serviceType = serviceType == null ? ClientServiceType.UNKNOWN : serviceType;
        deviceType = deviceType == null ? ClientDeviceType.UNKNOWN : deviceType;
        environment = environment == null ? ClientEnvironment.UNKNOWN : environment;
        source = source == null || source.isBlank() ? "unknown" : source.trim();
    }

    public static ClientRequestContext unknown(ClientDeviceType deviceType) {
        return new ClientRequestContext(
            ClientServiceType.UNKNOWN,
            deviceType,
            ClientEnvironment.UNKNOWN,
            "unknown",
            false
        );
    }
}
