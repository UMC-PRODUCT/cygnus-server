package com.umc.product.inhouse.application.port.out.query;

import java.util.List;
import java.util.Optional;

import com.umc.product.inhouse.domain.UmcProductMemberAccount;

public interface LoadUmcProductMemberAccountPort {

    Optional<UmcProductMemberAccount> findByMemberId(Long memberId);

    List<UmcProductMemberAccount> listByUmcProductMemberId(Long umcProductMemberId);

    boolean existsByMemberId(Long memberId);

    boolean existsByUmcProductMemberIdAndMemberId(Long umcProductMemberId, Long memberId);
}
