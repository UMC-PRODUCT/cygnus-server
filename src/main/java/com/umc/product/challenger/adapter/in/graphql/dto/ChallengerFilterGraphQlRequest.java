package com.umc.product.challenger.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.challenger.application.port.in.query.dto.SearchChallengerQuery;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;

public record ChallengerFilterGraphQlRequest(
    Long challengerId,
    String name,
    String nickname,
    String keyword,
    Long schoolId,
    Long chapterId,
    ChallengerPart part,
    Long gisuId,
    List<ChallengerStatus> statuses
) {

    public SearchChallengerQuery toQuery() {
        return new SearchChallengerQuery(
            challengerId,
            name,
            nickname,
            keyword,
            schoolId,
            chapterId,
            part,
            gisuId,
            statuses
        );
    }

    public static ChallengerFilterGraphQlRequest empty() {
        return new ChallengerFilterGraphQlRequest(null, null, null, null, null, null, null, null, null);
    }
}
