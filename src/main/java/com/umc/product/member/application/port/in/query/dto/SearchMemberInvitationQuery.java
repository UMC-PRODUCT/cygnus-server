package com.umc.product.member.application.port.in.query.dto;

import java.util.Set;

/**
 * 초대 가능한 회원 검색 조건입니다.
 *
 * @param keyword 회원 이름 검색어(앞뒤 공백은 제거됨)
 * @param excludedMemberIds 이미 초대된 회원 등 결과에서 제외할 회원 식별자
 * @param offset 결과 시작 위치
 * @param limit 결과 최대 개수(1~100)
 */
public record SearchMemberInvitationQuery(
    String keyword,
    Set<Long> excludedMemberIds,
    int offset,
    int limit
) {

    public SearchMemberInvitationQuery {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }

        keyword = normalizeKeyword(keyword);
        excludedMemberIds = excludedMemberIds == null ? Set.of() : Set.copyOf(excludedMemberIds);
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String normalized = keyword.strip();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.codePointCount(0, normalized.length()) > 80) {
            throw new IllegalArgumentException("keyword must be at most 80 code points");
        }
        return normalized;
    }
}
