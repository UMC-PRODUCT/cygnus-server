package com.umc.product.organization.application.port.in.query.dto.umcproduct;

import java.time.LocalDate;

import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;
import com.umc.product.organization.domain.enums.UmcProductPartRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;

public record UmcProductMemberSearchCondition(
    Long chapterId,
    Long partId,
    UmcProductPartRole partRole,
    UmcProductLeadershipRole leadershipRole,
    UmcProductPosition position,
    Long squadId,
    LocalDate activeOn
) {
    public static UmcProductMemberSearchCondition of(
        Long chapterId,
        Long partId,
        UmcProductPartRole partRole,
        UmcProductLeadershipRole leadershipRole,
        UmcProductPosition position,
        Long squadId,
        LocalDate activeOn
    ) {
        return new UmcProductMemberSearchCondition(
            chapterId,
            partId,
            partRole,
            leadershipRole,
            position,
            squadId,
            activeOn
        );
    }
}
