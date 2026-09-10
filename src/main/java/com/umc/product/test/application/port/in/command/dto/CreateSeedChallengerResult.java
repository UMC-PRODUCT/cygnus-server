package com.umc.product.test.application.port.in.command.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;

public record CreateSeedChallengerResult(
    Long challengerId,
    Long memberId,
    Long gisuId,
    ChallengerPart part,
    List<ChallengerTrack> tracks
) {

    public static CreateSeedChallengerResult of(
        Long challengerId,
        Long memberId,
        Long gisuId,
        ChallengerPart part,
        List<ChallengerTrack> tracks
    ) {
        return new CreateSeedChallengerResult(challengerId, memberId, gisuId, part, tracks);
    }
}
