package com.umc.product.member.application.port.in.query.dto;

import com.umc.product.common.domain.enums.ChallengerPart;

public record SearchMemberQuery(
    Long requesterMemberId,
    String keyword,
    Long gisuId,
    ChallengerPart part,
    Long chapterId,
    Long schoolId,
    SearchMemberAccessScope accessScope
) {

    public SearchMemberQuery {
        accessScope = accessScope == null ? SearchMemberAccessScope.all() : accessScope;
    }

    public SearchMemberQuery(String keyword, Long gisuId, ChallengerPart part, Long chapterId, Long schoolId) {
        this(null, keyword, gisuId, part, chapterId, schoolId, SearchMemberAccessScope.all());
    }

    public static SearchMemberQuery of(
        Long requesterMemberId,
        String keyword,
        Long gisuId,
        ChallengerPart part,
        Long chapterId,
        Long schoolId
    ) {
        return new SearchMemberQuery(
            requesterMemberId,
            keyword,
            gisuId,
            part,
            chapterId,
            schoolId,
            SearchMemberAccessScope.all()
        );
    }

    public SearchMemberQuery withAccessScope(SearchMemberAccessScope accessScope) {
        return new SearchMemberQuery(requesterMemberId, keyword, gisuId, part, chapterId, schoolId, accessScope);
    }
}
