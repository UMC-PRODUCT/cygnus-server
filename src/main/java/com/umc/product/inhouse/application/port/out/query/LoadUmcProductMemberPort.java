package com.umc.product.inhouse.application.port.out.query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.umc.product.inhouse.application.port.out.query.dto.UmcProductMemberSearchCriteria;
import com.umc.product.inhouse.domain.UmcProductMember;

public interface LoadUmcProductMemberPort {

    UmcProductMember getById(Long umcProductMemberId);

    UmcProductMember getByIdWithLock(Long umcProductMemberId);

    Optional<UmcProductMember> findById(Long umcProductMemberId);

    List<UmcProductMember> listByIds(Collection<Long> umcProductMemberIds);

    Page<Long> searchIds(UmcProductMemberSearchCriteria criteria, Pageable pageable);

}
