package com.umc.product.inhouse.application.port.out.query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.umc.product.inhouse.domain.UmcProductLeadership;
import com.umc.product.inhouse.domain.enums.UmcProductLeadershipRole;

public interface LoadUmcProductLeadershipPort {

    UmcProductLeadership getById(Long leadershipId);

    List<UmcProductLeadership> listByUmcProductMemberId(Long umcProductMemberId);

    List<UmcProductLeadership> listByUmcProductMemberIds(Collection<Long> umcProductMemberIds);

    boolean existsByMemberActivityPeriodId(Long memberActivityPeriodId);

    boolean existsByMemberIdAndRolesOnDate(
        Long memberId,
        Set<UmcProductLeadershipRole> roles,
        LocalDate activeOn
    );

    boolean existsOverlappingRole(
        UmcProductLeadershipRole role,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedLeadershipId
    );

    boolean existsOverlappingMember(
        Long umcProductMemberId,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedLeadershipId
    );
}
