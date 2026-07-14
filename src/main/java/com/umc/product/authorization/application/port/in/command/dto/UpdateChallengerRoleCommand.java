package com.umc.product.authorization.application.port.in.command.dto;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;

import lombok.Builder;

@Builder
public record UpdateChallengerRoleCommand(
    Long challengerRoleId,
    ChallengerRoleType roleType,
    Long organizationId,
    ChallengerPart responsiblePart,
    Long actorMemberId
) {
    public UpdateChallengerRoleCommand {
        if (challengerRoleId == null || roleType == null) {
            throw new IllegalArgumentException("변경할 challengerRoleId와 roleType은 필수입니다.");
        }
    }
}
