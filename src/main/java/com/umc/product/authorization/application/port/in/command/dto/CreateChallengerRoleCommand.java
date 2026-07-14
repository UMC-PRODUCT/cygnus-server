package com.umc.product.authorization.application.port.in.command.dto;

import com.umc.product.authorization.adapter.in.web.dto.request.CreateChallengerRoleRequest;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;

import lombok.Builder;

@Builder
public record CreateChallengerRoleCommand(
    Long challengerId,
    ChallengerRoleType roleType,
    Long organizationId,
    ChallengerPart responsiblePart,
    Long gisuId,
    Long actorMemberId
) {
    public CreateChallengerRoleCommand {
        if (challengerId == null || roleType == null || gisuId == null) {
            throw new IllegalArgumentException("챌린저 역할의 challengerId, roleType, gisuId는 필수입니다.");
        }
    }

    public static CreateChallengerRoleCommand of(
        CreateChallengerRoleRequest request,
        Long actorMemberId
    ) {
        return CreateChallengerRoleCommand.builder()
            .challengerId(request.challengerId())
            .roleType(request.roleType())
            .organizationId(request.organizationId())
            .responsiblePart(request.responsiblePart())
            .gisuId(request.gisuId())
            .actorMemberId(actorMemberId)
            .build();
    }

    public ChallengerRole toEntity() {
        return ChallengerRole.create(
            challengerId, roleType, organizationId, responsiblePart, gisuId
        );
    }
}
