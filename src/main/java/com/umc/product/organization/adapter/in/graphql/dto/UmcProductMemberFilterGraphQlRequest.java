package com.umc.product.organization.adapter.in.graphql.dto;

import java.time.LocalDate;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductMemberSearchCondition;
import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;

public record UmcProductMemberFilterGraphQlRequest(
    Long chapterId,
    UmcProductLeadershipRole leadershipRole,
    UmcProductPosition position,
    Long squadId,
    LocalDate activeOn
) {

    public UmcProductMemberSearchCondition toCondition() {
        return UmcProductMemberSearchCondition.of(
            chapterId,
            leadershipRole,
            position,
            squadId,
            activeOn
        );
    }

    public static UmcProductMemberSearchCondition toCondition(
        UmcProductMemberFilterGraphQlRequest request
    ) {
        return request == null
            ? UmcProductMemberSearchCondition.of(null, null, null, null, null)
            : request.toCondition();
    }
}
