package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.command.dto.SkipRecruitingInterviewCommand;

public record SkipRecruitingInterviewGraphQlRequest(
    String seasonId,
    String applicationId,
    String reason
) {

    public Long decodedSeasonId() {
        return GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON);
    }

    public Long decodedApplicationId() {
        return GlobalId.decodeLong(applicationId, GlobalIdTypes.RECRUITING_APPLICATION);
    }

    public SkipRecruitingInterviewCommand toCommand(Long skippedByMemberId) {
        return SkipRecruitingInterviewCommand.builder()
            .applicationId(decodedApplicationId())
            .skippedByMemberId(skippedByMemberId)
            .reason(reason)
            .build();
    }
}
