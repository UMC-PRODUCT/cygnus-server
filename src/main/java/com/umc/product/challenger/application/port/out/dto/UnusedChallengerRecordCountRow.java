package com.umc.product.challenger.application.port.out.dto;

/**
 * 기수×학교 단위로 그룹 집계한 미사용 챌린저 기록 코드 개수 (QueryDSL 프로젝션).
 *
 * @param gisuId      기수 ID
 * @param schoolId    학교 ID
 * @param unusedCount 해당 (기수, 학교)의 미사용(isUsed=false) 코드 개수
 */
public record UnusedChallengerRecordCountRow(
    Long gisuId,
    Long schoolId,
    long unusedCount
) {
}
