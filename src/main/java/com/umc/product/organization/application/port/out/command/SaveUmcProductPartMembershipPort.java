package com.umc.product.organization.application.port.out.command;

import com.umc.product.organization.domain.UmcProductPartMembership;

public interface SaveUmcProductPartMembershipPort {

    UmcProductPartMembership save(UmcProductPartMembership partMembership);

    void delete(UmcProductPartMembership partMembership);

    void deleteAllByUmcProductMemberId(Long umcProductMemberId);
}
