package com.umc.product.registry.application.port.out;

public interface RegistryAdvisoryLockPort {

    RegistryAdvisoryLock acquire(String registryName);

    interface RegistryAdvisoryLock extends AutoCloseable {

        @Override
        void close();
    }
}
