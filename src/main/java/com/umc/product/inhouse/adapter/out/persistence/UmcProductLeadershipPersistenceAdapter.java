package com.umc.product.inhouse.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.umc.product.inhouse.application.port.out.command.SaveUmcProductLeadershipPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductLeadershipPort;
import com.umc.product.inhouse.domain.UmcProductLeadership;
import com.umc.product.inhouse.domain.enums.UmcProductLeadershipRole;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UmcProductLeadershipPersistenceAdapter
    implements LoadUmcProductLeadershipPort, SaveUmcProductLeadershipPort {

    private static final Map<String, OrganizationErrorCode> CONSTRAINT_ERROR_CODES = Map.of(
        "ex_umc_product_leadership_member_dates",
        OrganizationErrorCode.UMC_PRODUCT_LEADERSHIP_OVERLAPPED,
        "ex_umc_product_leadership_role_dates",
        OrganizationErrorCode.UMC_PRODUCT_LEADERSHIP_OVERLAPPED
    );

    private final UmcProductLeadershipJpaRepository repository;

    @Override
    public UmcProductLeadership getById(Long leadershipId) {
        return repository.findById(leadershipId)
            .orElseThrow(() -> new OrganizationDomainException(
                OrganizationErrorCode.UMC_PRODUCT_LEADERSHIP_NOT_FOUND
            ));
    }

    @Override
    public List<UmcProductLeadership> listByUmcProductMemberId(Long umcProductMemberId) {
        return repository.findAllByUmcProductMemberId(umcProductMemberId);
    }

    @Override
    public List<UmcProductLeadership> listByUmcProductMemberIds(Collection<Long> umcProductMemberIds) {
        if (umcProductMemberIds == null || umcProductMemberIds.isEmpty()) {
            return List.of();
        }
        return repository.findAllByUmcProductMemberIds(umcProductMemberIds);
    }

    @Override
    public boolean existsByMemberActivityPeriodId(Long memberActivityPeriodId) {
        return repository.existsByMemberActivityPeriodId(memberActivityPeriodId);
    }

    @Override
    public boolean existsByMemberIdAndRolesOnDate(
        Long memberId,
        Set<UmcProductLeadershipRole> roles,
        LocalDate activeOn
    ) {
        if (memberId == null || roles == null || roles.isEmpty() || activeOn == null) {
            return false;
        }
        return repository.existsByMemberIdAndRolesOnDate(memberId, roles, activeOn);
    }

    @Override
    public boolean existsOverlappingRole(
        UmcProductLeadershipRole role,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedLeadershipId
    ) {
        return repository.existsOverlappingRole(role, startDate, endDate, excludedLeadershipId);
    }

    @Override
    public boolean existsOverlappingMember(
        Long umcProductMemberId,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedLeadershipId
    ) {
        return repository.existsOverlappingMember(
            umcProductMemberId,
            startDate,
            endDate,
            excludedLeadershipId
        );
    }

    @Override
    public UmcProductLeadership save(UmcProductLeadership leadership) {
        try {
            return repository.saveAndFlush(leadership);
        } catch (DataIntegrityViolationException exception) {
            throw UmcProductConstraintViolationTranslator.translate(exception, CONSTRAINT_ERROR_CODES);
        }
    }

    @Override
    public void delete(UmcProductLeadership leadership) {
        repository.delete(leadership);
    }

    @Override
    public void deleteAllByUmcProductMemberId(Long umcProductMemberId) {
        repository.deleteAllByUmcProductMemberId(umcProductMemberId);
    }
}
