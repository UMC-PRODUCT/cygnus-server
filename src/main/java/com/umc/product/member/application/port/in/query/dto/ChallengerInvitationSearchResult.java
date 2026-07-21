package com.umc.product.member.application.port.in.query.dto;

import java.util.List;

/**
 * 챌린저 초대 대상의 offset 페이지 결과입니다.
 */
public record ChallengerInvitationSearchResult(
    List<ChallengerInvitationInfo> items,
    Integer nextOffset,
    long total
) {

    public ChallengerInvitationSearchResult {
        items = items == null ? List.of() : List.copyOf(items);
        if (nextOffset != null && nextOffset < 0) {
            throw new IllegalArgumentException("nextOffset must not be negative");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
    }
}
