package com.umc.product.member.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;

/**
 * {@code members} 쿼리의 {@code MemberFilterInput}. 전역 ID 인자를 디코딩해 검색 쿼리로 변환한다.
 */
public record MemberFilterGraphQlRequest(
    String keyword,
    String gisuId,
    ChallengerPart part,
    String chapterId,
    String schoolId
) {

    public static MemberFilterGraphQlRequest empty() {
        return new MemberFilterGraphQlRequest(null, null, null, null, null);
    }

    public SearchMemberQuery toQuery() {
        return new SearchMemberQuery(
            keyword,
            decode(gisuId, GlobalIdTypes.GISU),
            part,
            decode(chapterId, GlobalIdTypes.CHAPTER),
            decode(schoolId, GlobalIdTypes.SCHOOL)
        );
    }

    private static Long decode(String value, String typeName) {
        return value == null ? null : GlobalId.decodeLong(value, typeName);
    }
}
