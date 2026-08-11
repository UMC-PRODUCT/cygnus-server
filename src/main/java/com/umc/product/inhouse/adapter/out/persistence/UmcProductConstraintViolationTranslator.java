package com.umc.product.inhouse.adapter.out.persistence;

import java.util.Map;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

final class UmcProductConstraintViolationTranslator {

    private UmcProductConstraintViolationTranslator() {}

    static RuntimeException translate(
        DataIntegrityViolationException exception,
        Map<String, OrganizationErrorCode> errorCodesByConstraint
    ) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolationException) {
                OrganizationErrorCode errorCode = getByConstraintName(
                    constraintViolationException.getConstraintName(), errorCodesByConstraint
                );
                if (errorCode != null) {
                    return new OrganizationDomainException(errorCode, exception);
                }
            }
            OrganizationErrorCode errorCode = getByConstraintMessage(cause.getMessage(), errorCodesByConstraint);
            if (errorCode != null) {
                return new OrganizationDomainException(errorCode, exception);
            }
            cause = cause.getCause();
        }
        return exception;
    }

    private static OrganizationErrorCode getByConstraintName(
        String constraintName,
        Map<String, OrganizationErrorCode> errorCodesByConstraint
    ) {
        return constraintName == null ? null : errorCodesByConstraint.get(constraintName);
    }

    private static OrganizationErrorCode getByConstraintMessage(
        String message,
        Map<String, OrganizationErrorCode> errorCodesByConstraint
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
