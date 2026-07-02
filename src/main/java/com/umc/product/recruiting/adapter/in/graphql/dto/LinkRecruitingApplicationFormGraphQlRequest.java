package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.LinkRecruitingApplicationFormCommand;

public record LinkRecruitingApplicationFormGraphQlRequest(
    Long formId,
    ChallengerTrack track
) {

    public LinkRecruitingApplicationFormCommand toCommand(Long roundId) {
        return LinkRecruitingApplicationFormCommand.builder()
            .roundId(roundId)
            .formId(formId)
            .track(track)
            .build();
    }
}
