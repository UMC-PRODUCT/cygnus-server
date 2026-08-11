package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingSeasonCommand;

public record UpdateRecruitingSeasonGraphQlRequest(
    String seasonId,
    String memo
) {

    public Long decodedSeasonId() {
        return GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON);
    }

    public UpdateRecruitingSeasonCommand toCommand() {
        return UpdateRecruitingSeasonCommand.builder()
            .seasonId(decodedSeasonId())
            .memo(memo)
            .build();
    }
}
