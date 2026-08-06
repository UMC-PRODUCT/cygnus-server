package com.umc.product.community.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.community.domain.Report;
import com.umc.product.community.domain.enums.ReportTargetType;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    long countByReporterIdAndTargetTypeAndTargetId(
        Long reporterId,
        ReportTargetType targetType,
        Long targetId
    );
}
