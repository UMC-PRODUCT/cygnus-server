package com.umc.product.organization.adapter.out.persistence.umcproduct;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductMemberSearchCondition;
import com.umc.product.organization.application.port.out.command.SaveUmcProductMemberPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.organization.domain.UmcProductMember;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UmcProductMemberPersistenceAdapter implements LoadUmcProductMemberPort, SaveUmcProductMemberPort {

    private static final Map<String, OrganizationErrorCode> CONSTRAINT_ERROR_CODES = Map.of(
        "uk_umc_product_member_member_id",
        OrganizationErrorCode.UMC_PRODUCT_MEMBER_ALREADY_EXISTS
    );

    private final UmcProductMemberJpaRepository umcProductMemberJpaRepository;
    private final UmcProductMemberQueryRepository umcProductMemberQueryRepository;

    @Override
    public UmcProductMember getById(Long umcProductMemberId) {
        return findById(umcProductMemberId)
            .orElseThrow(() -> new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_MEMBER_NOT_FOUND));
    }

    @Override
    public Optional<UmcProductMember> findById(Long umcProductMemberId) {
        return umcProductMemberJpaRepository.findById(umcProductMemberId);
    }

    @Override
    public UmcProductMember getByIdWithLock(Long umcProductMemberId) {
        return umcProductMemberJpaRepository.findByIdWithLock(umcProductMemberId)
            .orElseThrow(() -> new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_MEMBER_NOT_FOUND));
    }

    @Override
    public UmcProductMember getByMemberId(Long memberId) {
        return findByMemberId(memberId)
            .orElseThrow(() -> new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_MEMBER_NOT_FOUND));
    }

    @Override
    public Optional<UmcProductMember> findByMemberId(Long memberId) {
        return umcProductMemberJpaRepository.findByMemberId(memberId);
    }

    @Override
    public UmcProductMember getByMemberIdWithLock(Long memberId) {
        return umcProductMemberJpaRepository.findByMemberIdWithLock(memberId)
            .orElseThrow(() -> new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_MEMBER_NOT_FOUND));
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
    public boolean existsByMemberId(Long memberId) {
        return umcProductMemberJpaRepository.existsByMemberId(memberId);
    }

    @Override
    public UmcProductMember save(UmcProductMember member) {
        try {
            return umcProductMemberJpaRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException exception) {
            throw UmcProductConstraintViolationTranslator.translate(exception, CONSTRAINT_ERROR_CODES);
        }
    }

    @Override
    public void delete(UmcProductMember member) {
        umcProductMemberJpaRepository.delete(member);
    }
}
