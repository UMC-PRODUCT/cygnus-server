package com.umc.product.inhouse.application.port.in.query.dto;

import java.time.LocalDate;

import com.umc.product.inhouse.domain.enums.UmcProductLeadershipRole;
import com.umc.product.inhouse.domain.enums.UmcProductPosition;

public record UmcProductMemberSearchCondition(
    Long chapterId,
    UmcProductLeadershipRole leadershipRole,
    UmcProductPosition position,
    Long departmentId,
    LocalDate activeOn
) {
    public static UmcProductMemberSearchCondition of(
        Long chapterId,
        UmcProductLeadershipRole leadershipRole,
        UmcProductPosition position,
        Long departmentId,
        LocalDate activeOn
    ) {
        return new UmcProductMemberSearchCondition(
            chapterId,
            leadershipRole,
            position,
            departmentId,
            activeOn
        );
    }
}
