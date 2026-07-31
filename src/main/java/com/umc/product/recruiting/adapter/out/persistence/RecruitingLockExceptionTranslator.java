package com.umc.product.recruiting.adapter.out.persistence;

import java.sql.SQLException;
import java.util.function.Supplier;

import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;

final class RecruitingLockExceptionTranslator {

    private static final String LOCK_NOT_AVAILABLE_SQL_STATE = "55P03";
    private static final String DEADLOCK_DETECTED_SQL_STATE = "40P01";

    private RecruitingLockExceptionTranslator() {
    }

    static <T> T translate(Supplier<T> action) {
        try {
            return action.get();
        } catch (RuntimeException exception) {
            if (isConcurrencyLockFailure(exception)) {
                throw new RecruitingDomainException(
                    RecruitingErrorCode.RECRUITING_CONCURRENCY_LOCK_TIMEOUT
                );
            }
            throw exception;
        }
    }

    private static boolean isConcurrencyLockFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof LockTimeoutException || current instanceof PessimisticLockException) {
                return true;
            }
            if (current instanceof SQLException sqlException
                && (LOCK_NOT_AVAILABLE_SQL_STATE.equals(sqlException.getSQLState())
                || DEADLOCK_DETECTED_SQL_STATE.equals(sqlException.getSQLState()))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
