package com.umc.product.organization.application.port.out.command;

import com.umc.product.organization.domain.UmcProductLeadership;

public interface SaveUmcProductLeadershipPort {

    UmcProductLeadership save(UmcProductLeadership leadership);

    void delete(UmcProductLeadership leadership);

    void deleteAllByUmcProductMemberId(Long umcProductMemberId);
}
