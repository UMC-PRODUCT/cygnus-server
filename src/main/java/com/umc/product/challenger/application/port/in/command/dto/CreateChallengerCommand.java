package com.umc.product.challenger.application.port.in.command.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;

import lombok.Builder;

@Builder
public record CreateChallengerCommand(
    Long memberId,
    ChallengerPart part,
    List<ChallengerTrack> tracks,
    Long gisuId
) {
    public CreateChallengerCommand {
        tracks = tracks == null ? List.of() : List.copyOf(tracks);
    }
}
