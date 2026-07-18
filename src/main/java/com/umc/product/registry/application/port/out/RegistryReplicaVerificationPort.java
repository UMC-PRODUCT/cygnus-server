package com.umc.product.registry.application.port.out;

public interface RegistryReplicaVerificationPort {

    boolean isClean(String registryName);
}
