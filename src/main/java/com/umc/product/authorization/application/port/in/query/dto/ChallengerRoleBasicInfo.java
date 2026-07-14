package com.umc.product.authorization.application.port.in.query.dto;

import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

public record ChallengerRoleBasicInfo(
    ChallengerRoleType roleType,
    OrganizationType organizationType,
    Long organizationId
) {

    public static ChallengerRoleBasicInfo from(ChallengerRole role) {
        return new ChallengerRoleBasicInfo(
            role.getChallengerRoleType(),
            role.getOrganizationType(),
            role.getOrganizationId()
        );
    }
}
