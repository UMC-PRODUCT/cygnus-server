package com.umc.product.inhouse.application.service;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductLeadershipPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberAccountPort;
import com.umc.product.inhouse.domain.UmcProductMemberAccount;
import com.umc.product.inhouse.domain.enums.UmcProductLeadershipRole;

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
    private final LoadUmcProductMemberAccountPort loadUmcProductMemberAccountPort;
    private final UmcProductDateProvider umcProductDateProvider;

    public boolean canManageUmcProduct(Long requesterMemberId) {
        if (isCentralCoreInAnyGisu(requesterMemberId)) {
            return true;
        }
        if (requesterMemberId == null) {
            return false;
        }
        return loadUmcProductMemberAccountPort.findByMemberId(requesterMemberId)
            .map(UmcProductMemberAccount::getUmcProductMember)
            .map(member -> loadUmcProductLeadershipPort.existsByUmcProductMemberIdAndRolesOnDate(
                member.getId(),
                MANAGER_ROLES,
                umcProductDateProvider.today()
            ))
            .orElse(false);
    }

    public boolean canManageMemberProfile(
        Long requesterMemberId,
        Long targetUmcProductMemberId
    ) {
        if (requesterMemberId != null
            && targetUmcProductMemberId != null
            && loadUmcProductMemberAccountPort.existsByUmcProductMemberIdAndMemberId(
                targetUmcProductMemberId,
                requesterMemberId
            )) {
            return true;
        }
        return canManageUmcProduct(requesterMemberId);
    }

    private boolean isCentralCoreInAnyGisu(Long requesterMemberId) {
        return requesterMemberId != null && getChallengerRoleUseCase.isCentralCoreInAnyGisu(requesterMemberId);
    }
}
