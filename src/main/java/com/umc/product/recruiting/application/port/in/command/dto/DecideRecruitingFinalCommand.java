package com.umc.product.recruiting.application.port.in.command.dto;

import lombok.Builder;

@Builder
public record DecideRecruitingFinalCommand(
    Long applicationId,
    RecruitingDecisionStatus decision,
    Long decidedByMemberId,
    String reason
) {
}
