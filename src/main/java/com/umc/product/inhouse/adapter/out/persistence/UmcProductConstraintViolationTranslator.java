package com.umc.product.inhouse.adapter.out.persistence;

import java.util.Map;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import com.umc.product.inhouse.exception.InhouseDomainException;
import com.umc.product.inhouse.exception.InhouseErrorCode;

final class UmcProductConstraintViolationTranslator {

    private UmcProductConstraintViolationTranslator() {}

    static RuntimeException translate(
        DataIntegrityViolationException exception,
        Map<String, InhouseErrorCode> errorCodesByConstraint
    ) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolationException) {
                InhouseErrorCode errorCode = getByConstraintName(
                    constraintViolationException.getConstraintName(), errorCodesByConstraint
                );
                if (errorCode != null) {
                    return new InhouseDomainException(errorCode, exception);
                }
            }
            InhouseErrorCode errorCode = getByConstraintMessage(cause.getMessage(), errorCodesByConstraint);
            if (errorCode != null) {
                return new InhouseDomainException(errorCode, exception);
            }
            cause = cause.getCause();
        }
        return exception;
    }

    private static InhouseErrorCode getByConstraintName(
        String constraintName,
        Map<String, InhouseErrorCode> errorCodesByConstraint
    ) {
        return constraintName == null ? null : errorCodesByConstraint.get(constraintName);
    }

    private static InhouseErrorCode getByConstraintMessage(
        String message,
        Map<String, InhouseErrorCode> errorCodesByConstraint
    ) {
        if (message == null) {
            return null;
        }
        return errorCodesByConstraint.entrySet().stream()
            .filter(entry -> message.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }
}
