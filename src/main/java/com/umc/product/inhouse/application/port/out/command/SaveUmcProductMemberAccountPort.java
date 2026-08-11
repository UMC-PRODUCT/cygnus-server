package com.umc.product.inhouse.application.port.out.command;

import com.umc.product.inhouse.domain.UmcProductMemberAccount;

public interface SaveUmcProductMemberAccountPort {

    UmcProductMemberAccount save(UmcProductMemberAccount account);

    void delete(UmcProductMemberAccount account);

    void deleteAllByUmcProductMemberId(Long umcProductMemberId);
}
