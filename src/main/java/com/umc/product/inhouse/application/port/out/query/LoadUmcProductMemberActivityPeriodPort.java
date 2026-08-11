package com.umc.product.inhouse.application.port.out.query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.umc.product.inhouse.domain.UmcProductMemberActivityPeriod;

public interface LoadUmcProductMemberActivityPeriodPort {

    UmcProductMemberActivityPeriod getById(Long activityPeriodId);

    List<UmcProductMemberActivityPeriod> listByUmcProductMemberId(Long umcProductMemberId);

    List<UmcProductMemberActivityPeriod> listByUmcProductMemberIds(Collection<Long> umcProductMemberIds);

    Optional<UmcProductMemberActivityPeriod> findContaining(
        Long umcProductMemberId,
        LocalDate startDate,
        LocalDate endDate
    );

    boolean existsOverlappingOrAdjacent(
        Long umcProductMemberId,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedActivityPeriodId
    );
}
