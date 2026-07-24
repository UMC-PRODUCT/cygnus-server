package com.umc.product.authorization.application.port.out.policy;

import java.util.Objects;

public record PolicyClasspathResource(String logicalFilename, String classpathPath) {

    public PolicyClasspathResource {
        requireFilename(logicalFilename);
        requireClasspathPath(classpathPath);
    }

    private static void requireFilename(String value) {
        Objects.requireNonNull(value);
        if (value.isBlank() || value.contains("/") || value.contains("\\") || value.contains("..")) {
            throw new IllegalArgumentException("Policy logical filename is invalid");
        }
    }

    private static void requireClasspathPath(String value) {
        Objects.requireNonNull(value);
        if (value.isBlank()
            || value.startsWith("/")
            || value.contains("\\")
            || value.contains("..")
            || value.contains("://")) {
            throw new IllegalArgumentException("Policy classpath path is invalid");
        }
    }
}
