package com.umc.product.organization.application.port.out.command;

import com.umc.product.organization.domain.UmcProductMemberActivityPeriod;

public interface SaveUmcProductMemberActivityPeriodPort {

    UmcProductMemberActivityPeriod save(UmcProductMemberActivityPeriod activityPeriod);

    void delete(UmcProductMemberActivityPeriod activityPeriod);

    void deleteAllByUmcProductMemberId(Long umcProductMemberId);
}
