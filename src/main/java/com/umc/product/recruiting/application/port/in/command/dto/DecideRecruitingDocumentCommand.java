package com.umc.product.recruiting.application.port.in.command.dto;

import lombok.Builder;

@Builder
public record DecideRecruitingDocumentCommand(
    Long applicationId,
    RecruitingDecisionStatus decision,
    Long decidedByMemberId,
    String reason
) {
}
