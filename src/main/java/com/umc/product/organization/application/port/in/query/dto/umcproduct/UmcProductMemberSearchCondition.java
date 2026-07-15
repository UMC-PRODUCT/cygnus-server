package com.umc.product.organization.application.port.in.query.dto.umcproduct;

import java.time.LocalDate;

import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;

public record UmcProductMemberSearchCondition(
    Long chapterId,
    UmcProductLeadershipRole leadershipRole,
    UmcProductPosition position,
    Long squadId,
    LocalDate activeOn
) {
    public static UmcProductMemberSearchCondition of(
        Long chapterId,
        UmcProductLeadershipRole leadershipRole,
        UmcProductPosition position,
        Long squadId,
        LocalDate activeOn
    ) {
        return new UmcProductMemberSearchCondition(
            chapterId,
            leadershipRole,
            position,
            squadId,
            activeOn
        );
    }
}
