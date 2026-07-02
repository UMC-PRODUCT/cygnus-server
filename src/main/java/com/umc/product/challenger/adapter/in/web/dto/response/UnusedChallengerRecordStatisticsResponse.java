package com.umc.product.challenger.adapter.in.web.dto.response;

/**
 * 미사용 챌린저 기록 코드 통합 통계 응답 (CHALLENGER-RECORD-104).
 *
 * @param totalUnusedCount 아직 사용되지 않은(isUsed=false) 챌린저 기록 코드의 전체 개수
 */
public record UnusedChallengerRecordStatisticsResponse(
    long totalUnusedCount
) {
    public static UnusedChallengerRecordStatisticsResponse of(long totalUnusedCount) {
        return new UnusedChallengerRecordStatisticsResponse(totalUnusedCount);
    }
}
