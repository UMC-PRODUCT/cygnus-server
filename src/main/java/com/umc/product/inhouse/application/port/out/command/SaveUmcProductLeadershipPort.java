package com.umc.product.inhouse.application.port.out.command;

import com.umc.product.inhouse.domain.UmcProductLeadership;

public interface SaveUmcProductLeadershipPort {

    UmcProductLeadership save(UmcProductLeadership leadership);

    void delete(UmcProductLeadership leadership);

    void deleteAllByUmcProductMemberId(Long umcProductMemberId);
}
