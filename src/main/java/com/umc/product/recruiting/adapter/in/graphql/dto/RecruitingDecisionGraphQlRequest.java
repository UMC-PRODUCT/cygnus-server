package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingDocumentCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingFinalCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingDecisionStatus;

public record RecruitingDecisionGraphQlRequest(
    String seasonId,
    String applicationId,
    RecruitingDecisionStatus decision,
    ChallengerTrack acceptedTrack,
    String reason
) {

    public Long decodedSeasonId() {
        return GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON);
    }

    public Long decodedApplicationId() {
        return GlobalId.decodeLong(applicationId, GlobalIdTypes.RECRUITING_APPLICATION);
    }

    public DecideRecruitingDocumentCommand toDocumentCommand(Long decidedByMemberId) {
        return DecideRecruitingDocumentCommand.builder()
            .applicationId(decodedApplicationId())
            .decision(decision)
            .decidedByMemberId(decidedByMemberId)
            .reason(reason)
            .build();
    }

    public DecideRecruitingFinalCommand toFinalCommand(Long decidedByMemberId) {
        return DecideRecruitingFinalCommand.builder()
            .applicationId(decodedApplicationId())
            .decision(decision)
            .acceptedTrack(acceptedTrack)
            .decidedByMemberId(decidedByMemberId)
            .reason(reason)
            .build();
    }
}
