package com.umc.product.challenger.application.port.in.query.dto;

/**
 * 기수×학교 단위 미사용 챌린저 기록 코드 개수 (애플리케이션 계층 조회 결과).
 *
 * @param gisuId      기수 ID
 * @param schoolId    학교 ID
 * @param unusedCount 해당 (기수, 학교)의 미사용(isUsed=false) 코드 개수
 */
public record UnusedChallengerRecordCountInfo(
    Long gisuId,
    Long schoolId,
    long unusedCount
) {
}
