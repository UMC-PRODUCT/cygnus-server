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
    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

    private RecruitingLockExceptionTranslator() {
    }

    static <T> T translate(Supplier<T> action) {
        return translate(action, RecruitingErrorCode.RECRUITING_CONCURRENCY_LOCK_TIMEOUT);
    }

    static <T> T translateAssignment(Supplier<T> action) {
        try {
            return translate(action, RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_ASSIGNMENT_LOCK_TIMEOUT);
        } catch (RuntimeException exception) {
            if (hasSqlState(exception, UNIQUE_VIOLATION_SQL_STATE)) {
                throw new RecruitingDomainException(
                    RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_ASSIGNMENT_CONFLICT
                );
            }
            throw exception;
        }
    }

    private static <T> T translate(Supplier<T> action, RecruitingErrorCode lockErrorCode) {
        try {
            return action.get();
        } catch (RuntimeException exception) {
            if (isConcurrencyLockFailure(exception)) {
                throw new RecruitingDomainException(lockErrorCode);
            }
            throw exception;
        }
    }

    private static boolean isConcurrencyLockFailure(Throwable throwable) {
        return hasSqlState(throwable, LOCK_NOT_AVAILABLE_SQL_STATE)
            || hasSqlState(throwable, DEADLOCK_DETECTED_SQL_STATE)
            || hasLockException(throwable);
    }

    private static boolean hasLockException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof LockTimeoutException || current instanceof PessimisticLockException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean hasSqlState(Throwable throwable, String expectedSqlState) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException
                && expectedSqlState.equals(sqlException.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
