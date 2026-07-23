package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingSeasonCommand;

public record CreateRecruitingSeasonGraphQlRequest(
    Long gisuId,
    Long schoolId,
    List<RecruitingSeasonTrackQuotaGraphQlRequest> quotas
) {

    public CreateRecruitingSeasonCommand toCommand(Long requesterMemberId) {
        return CreateRecruitingSeasonCommand.builder()
            .requesterMemberId(requesterMemberId)
            .gisuId(gisuId)
            .schoolId(schoolId)
            .quotas(quotas == null
                ? List.of()
                : quotas.stream().map(RecruitingSeasonTrackQuotaGraphQlRequest::toCommand).toList())
            .build();
    }
}
