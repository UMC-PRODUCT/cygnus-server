package com.umc.product.inhouse.application.port.in.query;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductMemberInfo;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductMemberSearchCondition;

public interface GetUmcProductMemberUseCase {

    UmcProductMemberInfo getById(Long umcProductMemberId);

    Optional<UmcProductMemberInfo> findByAccountMemberId(Long memberId);

    Page<UmcProductMemberInfo> search(UmcProductMemberSearchCondition condition, Pageable pageable);
}
