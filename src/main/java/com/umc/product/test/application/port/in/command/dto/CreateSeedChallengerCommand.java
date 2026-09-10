package com.umc.product.test.application.port.in.command.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;

public record CreateSeedChallengerCommand(
    Long memberId,
    Long gisuId,
    ChallengerPart part,
    List<ChallengerTrack> tracks
) {
    public CreateSeedChallengerCommand {
        tracks = tracks == null ? List.of() : tracks.stream().distinct().toList();
    }

    public static CreateSeedChallengerCommand of(
        Long memberId, Long gisuId, ChallengerPart part, List<ChallengerTrack> tracks
    ) {
        return new CreateSeedChallengerCommand(memberId, gisuId, part, tracks);
    }
}
