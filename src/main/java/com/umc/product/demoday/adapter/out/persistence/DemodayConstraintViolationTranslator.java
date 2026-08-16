package com.umc.product.demoday.adapter.out.persistence;

import java.util.Map;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

/**
 * 알려진 DB 제약 위반만 데모데이 비즈니스 오류로 변환한다.
 * 동시 요청을 포함해 알 수 없는 영속성 오류는 기술 오류로 그대로 전파한다.
 */
final class DemodayConstraintViolationTranslator {

    private DemodayConstraintViolationTranslator() {
    }

    static RuntimeException translate(
        DataIntegrityViolationException exception,
        Map<String, DemodayErrorCode> errorCodesByConstraint
    ) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolationException) {
                DemodayErrorCode errorCode = errorCodesByConstraint.get(
                    constraintViolationException.getConstraintName()
                );
                if (errorCode != null) {
                    return new DemodayDomainException(errorCode, exception);
                }
            }

            DemodayErrorCode errorCode = getByConstraintMessage(cause.getMessage(), errorCodesByConstraint);
            if (errorCode != null) {
                return new DemodayDomainException(errorCode, exception);
            }
            cause = cause.getCause();
        }
        return exception;
    }

    private static DemodayErrorCode getByConstraintMessage(
        String message,
        Map<String, DemodayErrorCode> errorCodesByConstraint
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
