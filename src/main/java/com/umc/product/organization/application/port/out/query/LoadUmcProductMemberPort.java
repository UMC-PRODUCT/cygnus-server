package com.umc.product.organization.application.port.out.query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductMemberSearchCondition;
import com.umc.product.organization.domain.UmcProductMember;

public interface LoadUmcProductMemberPort {

    UmcProductMember getById(Long umcProductMemberId);

    UmcProductMember getByIdWithLock(Long umcProductMemberId);

    Optional<UmcProductMember> findById(Long umcProductMemberId);

    UmcProductMember getByMemberId(Long memberId);

    UmcProductMember getByMemberIdWithLock(Long memberId);

    Optional<UmcProductMember> findByMemberId(Long memberId);

    List<UmcProductMember> listByIds(Collection<Long> umcProductMemberIds);

    Page<Long> searchIds(UmcProductMemberSearchCondition condition, Pageable pageable);

    boolean existsByMemberId(Long memberId);
}
