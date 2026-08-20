package com.umc.product.demoday.application.port.out;

import java.util.Set;

/**
 * 관리자 투표 기록 검색 조건.
 *
 * <p>{@code memberIds}가 {@code null}이면 회원명 필터를 적용하지 않는다. 빈 집합은 검색 결과가
 * 없다는 뜻이므로 호출자가 영속성 조회를 생략한다.
 */
public record DemodayVoteSearchCondition(
    Long pollId,
    Long cursor,
    Long boothId,
    Set<Long> memberIds,
    int limit
) {
}
