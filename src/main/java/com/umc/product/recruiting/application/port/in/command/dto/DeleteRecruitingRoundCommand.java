package com.umc.product.recruiting.application.port.in.command.dto;

import lombok.Builder;

@Builder
public record DeleteRecruitingRoundCommand(
    Long seasonId,
    Long roundId,
    Long requesterMemberId
) {
}
