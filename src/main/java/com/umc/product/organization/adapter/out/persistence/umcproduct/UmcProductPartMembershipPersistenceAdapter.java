package com.umc.product.organization.adapter.out.persistence.umcproduct;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.umc.product.organization.application.port.out.command.SaveUmcProductPartMembershipPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductPartMembershipPort;
import com.umc.product.organization.domain.UmcProductPartMembership;
import com.umc.product.organization.domain.enums.UmcProductPartRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UmcProductPartMembershipPersistenceAdapter
    implements LoadUmcProductPartMembershipPort, SaveUmcProductPartMembershipPort {

    private static final Map<String, OrganizationErrorCode> CONSTRAINT_ERROR_CODES = Map.of(
        "ex_umc_product_part_membership_activity",
        OrganizationErrorCode.UMC_PRODUCT_PART_MEMBERSHIP_OVERLAPPED,
        "ex_umc_product_part_lead_dates",
        OrganizationErrorCode.UMC_PRODUCT_PART_LEAD_OVERLAPPED
    );

    private final UmcProductPartMembershipJpaRepository repository;

    @Override
    public UmcProductPartMembership getById(Long partMembershipId) {
        return repository.findById(partMembershipId)
            .orElseThrow(() -> new OrganizationDomainException(
                OrganizationErrorCode.UMC_PRODUCT_PART_MEMBERSHIP_NOT_FOUND
            ));
    }

    @Override
    public List<UmcProductPartMembership> listByUmcProductMemberId(Long umcProductMemberId) {
        return repository.findAllByUmcProductMemberId(umcProductMemberId);
    }

    @Override
    public List<UmcProductPartMembership> listByUmcProductMemberIds(Collection<Long> umcProductMemberIds) {
        if (umcProductMemberIds == null || umcProductMemberIds.isEmpty()) {
            return List.of();
        }
        return repository.findAllByUmcProductMemberIds(umcProductMemberIds);
    }

    @Override
    public boolean existsByPartId(Long partId) {
        return repository.existsByPartId(partId);
    }

    @Override
    public boolean existsByMemberActivityPeriodId(Long memberActivityPeriodId) {
        return repository.existsByMemberActivityPeriodId(memberActivityPeriodId);
    }

    @Override
    public boolean existsOverlappingSameAssignment(
        Long umcProductMemberId,
        Long partId,
        UmcProductPartRole role,
        UmcProductPosition position,
        String responsibilityTitle,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedPartMembershipId
    ) {
        return repository.existsOverlappingSameAssignment(
            umcProductMemberId,
            partId,
            role,
            position,
            responsibilityTitle,
            startDate,
            endDate,
            excludedPartMembershipId
        );
    }

    @Override
    public boolean existsOverlappingPartLead(
        Long partId,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedPartMembershipId
    ) {
        return repository.existsOverlappingPartLead(
            partId,
            UmcProductPartRole.PART_LEAD,
            startDate,
            endDate,
            excludedPartMembershipId
        );
    }

    @Override
    public UmcProductPartMembership save(UmcProductPartMembership partMembership) {
        try {
            return repository.saveAndFlush(partMembership);
        } catch (DataIntegrityViolationException exception) {
            throw UmcProductConstraintViolationTranslator.translate(exception, CONSTRAINT_ERROR_CODES);
        }
    }

    @Override
    public void delete(UmcProductPartMembership partMembership) {
        repository.delete(partMembership);
    }

    @Override
    public void deleteAllByUmcProductMemberId(Long umcProductMemberId) {
        repository.deleteAllByUmcProductMemberId(umcProductMemberId);
    }
}
