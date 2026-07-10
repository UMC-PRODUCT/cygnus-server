package com.umc.product.member.application.port.in.query;

import org.springframework.data.domain.Pageable;

import com.umc.product.member.application.port.in.query.dto.ChallengerSearchV2Result;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;
import com.umc.product.member.application.port.in.query.dto.SearchMemberResult;
import com.umc.product.member.application.port.in.query.dto.SearchMemberV2Result;

public interface SearchMemberUseCase {

    SearchMemberResult searchBy(SearchMemberQuery query, Long requesterMemberId, Pageable pageable);

    /**
     * v2 회원 검색 — 같은 회원의 여러 기수 챌린저 이력은 1개 row로 묶입니다.
     * currentChallenger와 challengerRecords 요약을 함께 제공합니다.
     */
    SearchMemberV2Result searchByV2(SearchMemberQuery query, Long requesterMemberId, Pageable pageable);

    /**
     * v2 챌린저 검색 — 챌린저 단위 페이지네이션. 같은 회원의 여러 기수 챌린저 이력은
     * 각각 별도 row로 반환됩니다. v1 검색 응답 + challengerStatus + isAdminInActiveGisu 필드를 포함합니다.
     */
    ChallengerSearchV2Result searchChallengersByV2(
        SearchMemberQuery query,
        Long requesterMemberId,
        Pageable pageable
    );
}
