package com.umc.product.member.application.port.in.query.dto;

import java.util.List;

/**
 * 회원 초대 대상의 offset 페이지 결과입니다.
 */
public record MemberInvitationSearchResult(
    List<MemberInvitationInfo> items,
    Integer nextOffset,
    long total
) {

    public MemberInvitationSearchResult {
        items = items == null ? List.of() : List.copyOf(items);
        if (nextOffset != null && nextOffset < 0) {
            throw new IllegalArgumentException("nextOffset must not be negative");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
    }
}
