package com.umc.product.global.persistence;

import java.util.Locale;

import org.hibernate.exception.ConstraintViolationException;

public final class ConstraintViolationInspector {

    private ConstraintViolationInspector() {}

    public static boolean matches(Throwable throwable, String constraintName) {
        String expected = normalize(constraintName);
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                && expected.equals(normalize(violation.getConstraintName()))) {
                return true;
            }
            if (normalize(cause.getMessage()).contains(expected)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
