package com.umc.product.inhouse.application.port.out.command;

import com.umc.product.inhouse.domain.UmcProductMemberActivityPeriod;

public interface SaveUmcProductMemberActivityPeriodPort {

    UmcProductMemberActivityPeriod save(UmcProductMemberActivityPeriod activityPeriod);

    void delete(UmcProductMemberActivityPeriod activityPeriod);

    void deleteAllByUmcProductMemberId(Long umcProductMemberId);
}
