package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.LinkRecruitingApplicationFormCommand;

import jakarta.validation.constraints.NotNull;

public record LinkRecruitingApplicationFormRequest(
    @NotNull Long formId,
    @NotNull ChallengerTrack track
) {

    public LinkRecruitingApplicationFormCommand toCommand(Long roundId) {
        return LinkRecruitingApplicationFormCommand.builder()
            .roundId(roundId)
            .formId(formId)
            .track(track)
            .build();
    }
}
