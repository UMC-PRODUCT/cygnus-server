package com.umc.product.challenger.adapter.in.web.dto.response;

import java.util.List;

/**
 * 미사용 챌린저 기록 코드 통계 응답 (CHALLENGER-RECORD-104).
 * <p>
 * 기수×학교 단위로 그룹 집계한 미사용(isUsed=false) 코드 개수 목록과 전체 합계를 담는다.
 *
 * @param totalUnusedCount 모든 행의 미사용 코드 개수 합계 (전체 미사용 코드 수)
 * @param rows             기수×학교별 미사용 코드 개수 행 목록 (기수 내림차순, 학교 오름차순)
 */
public record UnusedChallengerRecordStatisticsResponse(
    long totalUnusedCount,
    List<Row> rows
) {
    public static UnusedChallengerRecordStatisticsResponse of(long totalUnusedCount, List<Row> rows) {
        return new UnusedChallengerRecordStatisticsResponse(totalUnusedCount, rows);
    }

    /**
     * 기수×학교 단위 미사용 코드 개수 행.
     *
     * @param gisuId      기수 ID
     * @param generation  기수 세대 (예: 9)
     * @param schoolId    학교 ID
     * @param schoolName  학교명
     * @param unusedCount 해당 (기수, 학교)의 미사용 코드 개수
     */
    public record Row(
        Long gisuId,
        Long generation,
        Long schoolId,
        String schoolName,
        long unusedCount
    ) {
    }
}
