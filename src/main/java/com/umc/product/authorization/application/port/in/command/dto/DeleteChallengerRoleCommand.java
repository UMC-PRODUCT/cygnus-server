package com.umc.product.authorization.application.port.in.command.dto;

import lombok.Builder;

@Builder
public record DeleteChallengerRoleCommand(
    Long challengerRoleId,
    Long actorMemberId
) {
    public DeleteChallengerRoleCommand {
        if (challengerRoleId == null) {
            throw new IllegalArgumentException("해제할 challengerRoleId는 필수입니다.");
        }
    }
}
