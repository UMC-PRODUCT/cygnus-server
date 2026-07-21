package com.umc.product.community.adapter.out.persistence;

import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.umc.product.community.application.port.out.report.LoadReportPort;
import com.umc.product.community.application.port.out.report.SaveReportPort;
import com.umc.product.community.domain.Report;
import com.umc.product.community.domain.enums.ReportTargetType;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportPersistenceAdapter implements LoadReportPort, SaveReportPort {

    private static final String REPORTER_TARGET_DUPLICATE_CONSTRAINT =
        "uq_report_reporter_target";

    private final ReportRepository reportRepository;

    @Override
    public boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType,
                                                              Long targetId) {
        return reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, targetType, targetId);
    }

    @Override
    public boolean existsThreadMessageReport(Long reporterId, Long messageId) {
        return reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
            reporterId,
            ReportTargetType.THREAD_MESSAGE,
            messageId
        );
    }

    @Override
    public Optional<Report> findById(Long reportId) {
        return reportRepository.findById(reportId);
    }

    @Override
    public Report save(Report report) {
        try {
            return reportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException exception) {
            if (isReporterTargetDuplicate(exception)) {
                throw new CommunityDomainException(CommunityErrorCode.REPORT_ALREADY_EXISTS, exception);
            }
            throw exception;
        }
    }

    private boolean isReporterTargetDuplicate(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolationException
                && REPORTER_TARGET_DUPLICATE_CONSTRAINT.equals(constraintViolationException.getConstraintName())) {
                return true;
            }
            if (cause.getMessage() != null
                && cause.getMessage().contains(REPORTER_TARGET_DUPLICATE_CONSTRAINT)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
