package com.umc.product.inhouse.application.port.out.command;

import com.umc.product.inhouse.domain.UmcProductMember;

public interface SaveUmcProductMemberPort {

    UmcProductMember save(UmcProductMember member);

    void delete(UmcProductMember member);
}
