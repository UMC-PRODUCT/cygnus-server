package com.umc.product.test.application.port.in.command.dto;

import java.util.List;

/**
 * 벌크 시딩 결과. memberIds 는 k6 seed.json 산출용 샘플이다 (전체가 아님).
 */
public record SeedBulkDataResult(
    Long gisuId,
    int memberCount,
    int challengerCount,
    long pointCount,
    int scheduleCount,
    long participantCount,
    int noticeCount,
    List<Long> memberIds
) {
}
