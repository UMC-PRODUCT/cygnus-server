package com.umc.product.community.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.umc.product.community.domain.Report;
import com.umc.product.community.domain.enums.ReportReason;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportPersistenceAdapter 신고 중복 무결성 예외 변환")
class ReportPersistenceAdapterConstraintTranslationTest {

    private static final String DUPLICATE_CONSTRAINT = "uq_report_thread_message_reporter_target";

    @Mock
    ReportRepository reportRepository;

    ReportPersistenceAdapter sut;

    @BeforeEach
    void setUp() {
        sut = new ReportPersistenceAdapter(reportRepository);
    }

    @Test
    @DisplayName("스레드 메시지 중복 unique 위반만 REPORT_ALREADY_EXISTS로 변환한다")
    void save_스레드_메시지_중복_unique만_도메인_충돌로_변환한다() {
        DataIntegrityViolationException exception = duplicateViolation();
        given(reportRepository.saveAndFlush(any(Report.class))).willThrow(exception);

        assertThatThrownBy(() -> sut.save(report()))
            .isInstanceOfSatisfying(CommunityDomainException.class, domainException -> {
                assertThat(domainException.getBaseCode()).isEqualTo(CommunityErrorCode.REPORT_ALREADY_EXISTS);
                assertThat(domainException.getCause()).isSameAs(exception);
            });
    }

    @Test
    @DisplayName("다른 무결성 위반은 원래 DataIntegrityViolationException을 그대로 전달한다")
    void save_다른_무결성_위반은_그대로_전달한다() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
            "duplicate key value violates unique constraint \"other_constraint\""
        );
        given(reportRepository.saveAndFlush(any(Report.class))).willThrow(exception);

        assertThatThrownBy(() -> sut.save(report()))
            .isSameAs(exception);
    }

    private DataIntegrityViolationException duplicateViolation() {
        ConstraintViolationException constraintViolation = new ConstraintViolationException(
            "duplicate key",
            new SQLException("duplicate key"),
            DUPLICATE_CONSTRAINT
        );
        return new DataIntegrityViolationException("report insert failed", constraintViolation);
    }

    private Report report() {
        return Report.createThreadMessage(10L, 20L, 30L, ReportReason.SPAM);
    }
}
