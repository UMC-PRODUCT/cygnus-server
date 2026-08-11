package com.umc.product.inhouse.application.port.out.query.dto;

import java.time.LocalDate;
import java.util.Set;

import com.umc.product.inhouse.domain.enums.UmcProductLeadershipRole;
import com.umc.product.inhouse.domain.enums.UmcProductPosition;

public record UmcProductMemberSearchCriteria(
    Long chapterId,
    UmcProductLeadershipRole leadershipRole,
    UmcProductPosition position,
    Set<Long> departmentIds,
    LocalDate activeOn
) {
    public UmcProductMemberSearchCriteria {
        departmentIds = departmentIds == null ? null : Set.copyOf(departmentIds);
    }
}
