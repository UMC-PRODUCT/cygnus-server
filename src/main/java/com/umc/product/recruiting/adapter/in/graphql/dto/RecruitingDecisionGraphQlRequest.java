package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingDocumentCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingFinalCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingDecisionStatus;

public record RecruitingDecisionGraphQlRequest(
    RecruitingDecisionStatus decision,
    ChallengerTrack acceptedTrack,
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
            .acceptedTrack(acceptedTrack)
            .decidedByMemberId(decidedByMemberId)
            .reason(reason)
            .build();
    }
}
