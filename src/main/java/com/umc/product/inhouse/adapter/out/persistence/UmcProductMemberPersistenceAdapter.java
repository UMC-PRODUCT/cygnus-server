package com.umc.product.inhouse.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductMemberSearchCondition;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductMemberPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.inhouse.domain.UmcProductMember;
import com.umc.product.inhouse.exception.InhouseDomainException;
import com.umc.product.inhouse.exception.InhouseErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UmcProductMemberPersistenceAdapter implements LoadUmcProductMemberPort, SaveUmcProductMemberPort {

    private final UmcProductMemberJpaRepository umcProductMemberJpaRepository;
    private final UmcProductMemberQueryRepository umcProductMemberQueryRepository;

    @Override
    public UmcProductMember getById(Long umcProductMemberId) {
        return findById(umcProductMemberId)
            .orElseThrow(() -> new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_MEMBER_NOT_FOUND));
    }

    @Override
    public Optional<UmcProductMember> findById(Long umcProductMemberId) {
        return umcProductMemberJpaRepository.findById(umcProductMemberId);
    }

    @Override
    public UmcProductMember getByIdWithLock(Long umcProductMemberId) {
        return umcProductMemberJpaRepository.findByIdWithLock(umcProductMemberId)
            .orElseThrow(() -> new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_MEMBER_NOT_FOUND));
    }

    @Override
    public List<UmcProductMember> listByIds(Collection<Long> umcProductMemberIds) {
        if (umcProductMemberIds == null || umcProductMemberIds.isEmpty()) {
            return List.of();
        }
        return umcProductMemberJpaRepository.findByIdIn(umcProductMemberIds);
    }

    @Override
    public Page<Long> searchIds(UmcProductMemberSearchCondition condition, Pageable pageable) {
        return umcProductMemberQueryRepository.searchMemberIds(condition, pageable);
    }

    @Override
    public UmcProductMember save(UmcProductMember member) {
        return umcProductMemberJpaRepository.saveAndFlush(member);
    }

    @Override
    public void delete(UmcProductMember member) {
        umcProductMemberJpaRepository.delete(member);
    }
}
