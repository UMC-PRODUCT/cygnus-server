package com.umc.product.member.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.umc.product.challenger.domain.Challenger;
import com.umc.product.member.application.dto.MemberSearchAccessScope;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;

public interface SearchMemberPort {

    /**
     * 챌린저 단위 검색 (v1). 같은 회원의 여러 기수 챌린저가 각각 별도 row로 반환됩니다.
     */
    Page<Challenger> search(SearchMemberQuery query, Pageable pageable);

    /**
     * 회원 단위 검색 (v2). 같은 회원이 여러 챌린저 이력을 가지더라도 1개 row(memberId)로만 반환합니다.
     */
    Page<Long> searchMemberIds(SearchMemberQuery query, Pageable pageable);

    Page<Long> searchMemberIds(SearchMemberQuery query, MemberSearchAccessScope scope, Pageable pageable);
}
