package com.umc.product.registry.domain;

public class RegistryBackfillInterruptedException extends RuntimeException {

    public RegistryBackfillInterruptedException(String message) {
        super(message);
    }

    public RegistryBackfillInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }
}
