package com.umc.product.member.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;

public record MemberSearchGraphQlRequest(
    String keyword,
    Long gisuId,
    ChallengerPart part,
    Long chapterId,
    Long schoolId
) {

    public SearchMemberQuery toQuery() {
        return new SearchMemberQuery(keyword, gisuId, part, chapterId, schoolId);
    }
}
