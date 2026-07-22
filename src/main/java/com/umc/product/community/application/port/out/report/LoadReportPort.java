package com.umc.product.community.application.port.out.report;

import java.util.Optional;

import com.umc.product.community.domain.Report;
import com.umc.product.community.domain.enums.ReportTargetType;

public interface LoadReportPort {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    boolean existsThreadMessageReport(Long reporterId, Long messageId);

    Optional<Report> findById(Long reportId);
}
