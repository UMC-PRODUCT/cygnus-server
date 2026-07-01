package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingDocumentCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingFinalCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingDecisionStatus;

import jakarta.validation.constraints.NotNull;

public record RecruitingDecisionRequest(
    @NotNull RecruitingDecisionStatus decision,
    String reason
) {

    public DecideRecruitingDocumentCommand toDocumentCommand(Long applicationId, Long decidedByMemberId) {
        return DecideRecruitingDocumentCommand.builder()
            .applicationId(applicationId)
            .decision(decision)
            .decidedByMemberId(decidedByMemberId)
            .reason(reason)
            .build();
    }

    public DecideRecruitingFinalCommand toFinalCommand(Long applicationId, Long decidedByMemberId) {
        return DecideRecruitingFinalCommand.builder()
            .applicationId(applicationId)
            .decision(decision)
            .decidedByMemberId(decidedByMemberId)
            .reason(reason)
            .build();
    }
}
