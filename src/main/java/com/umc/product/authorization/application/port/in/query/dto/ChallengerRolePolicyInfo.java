package com.umc.product.authorization.application.port.in.query.dto;

import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

public record ChallengerRolePolicyInfo(
    ChallengerRoleType roleType,
    OrganizationType organizationType,
    Long organizationId,
    ChallengerPart responsiblePart,
    Long gisuId
) {

    public static ChallengerRolePolicyInfo from(ChallengerRole role) {
        return new ChallengerRolePolicyInfo(
            role.getChallengerRoleType(),
            role.getOrganizationType(),
            role.getOrganizationId(),
            role.getResponsiblePart(),
            role.getGisuId()
        );
    }
}
