package com.umc.product.organization.application.service;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.organization.application.port.out.query.LoadUmcProductLeadershipPort;
import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UmcProductAccessPolicy {

    public static final Set<UmcProductLeadershipRole> MANAGER_ROLES = Set.of(
        UmcProductLeadershipRole.UMC_PRODUCT_LEAD,
        UmcProductLeadershipRole.UMC_PRODUCT_VICE_LEAD
    );

    private final GetChallengerRoleUseCase getChallengerRoleUseCase;
    private final LoadUmcProductLeadershipPort loadUmcProductLeadershipPort;
    private final UmcProductDateProvider umcProductDateProvider;

    public boolean canManageUmcProduct(Long requesterMemberId) {
        if (isCentralCore(requesterMemberId)) {
            return true;
        }
        if (requesterMemberId == null) {
            return false;
        }
        LocalDate today = umcProductDateProvider.today();
        return loadUmcProductLeadershipPort.existsByMemberIdAndRolesOnDate(
            requesterMemberId,
            MANAGER_ROLES,
            today
        );
    }

    public boolean canManageMemberProfile(
        Long requesterMemberId,
        Long targetMemberId
    ) {
        if (requesterMemberId != null && requesterMemberId.equals(targetMemberId)) {
            return true;
        }
        return canManageUmcProduct(requesterMemberId);
    }

    @SuppressWarnings("removal")
    private boolean isCentralCore(Long requesterMemberId) {
        return requesterMemberId != null && getChallengerRoleUseCase.isCentralCore(requesterMemberId);
    }
}
