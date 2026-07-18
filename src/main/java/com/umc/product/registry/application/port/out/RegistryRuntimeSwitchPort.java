package com.umc.product.registry.application.port.out;

public interface RegistryRuntimeSwitchPort {

    void enableCleanupAndEnforcement();

    void disableCleanupAndEnforcement();
}
