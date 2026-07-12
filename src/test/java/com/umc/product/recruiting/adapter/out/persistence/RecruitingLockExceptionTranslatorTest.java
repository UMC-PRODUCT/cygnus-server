package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

class RecruitingLockExceptionTranslatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"55P03", "40P01"})
    @DisplayName("PostgreSQL lock timeout과 deadlock은 재시도 가능한 Recruiting conflict로 변환한다")
    void translatePostgreSqlConcurrencyFailures(String sqlState) {
        RuntimeException persistenceFailure = new RuntimeException(
            new SQLException("concurrency failure", sqlState)
        );

        assertThatThrownBy(() -> RecruitingLockExceptionTranslator.translate(() -> {
            throw persistenceFailure;
        }))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_CONCURRENCY_LOCK_TIMEOUT);
    }
}
