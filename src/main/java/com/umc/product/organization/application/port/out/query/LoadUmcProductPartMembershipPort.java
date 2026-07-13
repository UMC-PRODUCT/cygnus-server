package com.umc.product.organization.application.port.out.query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.umc.product.organization.domain.UmcProductPartMembership;
import com.umc.product.organization.domain.enums.UmcProductPartRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;

public interface LoadUmcProductPartMembershipPort {

    UmcProductPartMembership getById(Long partMembershipId);

    List<UmcProductPartMembership> listByUmcProductMemberId(Long umcProductMemberId);

    List<UmcProductPartMembership> listByUmcProductMemberIds(Collection<Long> umcProductMemberIds);

    boolean existsByPartId(Long partId);

    boolean existsByMemberActivityPeriodId(Long memberActivityPeriodId);

    boolean existsOverlappingSameAssignment(
        Long umcProductMemberId,
        Long partId,
        UmcProductPartRole role,
        UmcProductPosition position,
        String responsibilityTitle,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedPartMembershipId
    );

    boolean existsOverlappingPartLead(
        Long partId,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedPartMembershipId
    );
}
