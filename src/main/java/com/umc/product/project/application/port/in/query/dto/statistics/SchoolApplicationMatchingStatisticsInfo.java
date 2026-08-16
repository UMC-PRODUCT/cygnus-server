package com.umc.product.project.application.port.in.query.dto.statistics;

/**
 * 학교별 지원 가능 총원, 지원 완료 인원 수, 매칭 완료 인원 수.
 */
public record SchoolApplicationMatchingStatisticsInfo(
    Long schoolId,
    long matchedMemberCount,
    long totalMemberCount,
    long appliedMemberCount
) {
}
