package com.umc.product.challenger.application.port.in.command.dto;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;

import lombok.Builder;

@Builder
public record CreateChallengerCommand(
    Long memberId,
    ChallengerPart part,
    ChallengerTrack track,
    Long gisuId
) {
}
