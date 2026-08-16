package com.umc.product.challenger.application.port.in.command.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;

public record AddChallengerTrackCommand(
    Long memberId,
    Long gisuId,
    ChallengerTrack track
) {
    public static AddChallengerTrackCommand of(Long memberId, Long gisuId, ChallengerTrack track) {
        return new AddChallengerTrackCommand(memberId, gisuId, track);
    }
}
