package com.umc.product.test.adapter.in.web.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.test.application.port.in.command.dto.SeedChallengersCommand;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SeedChallengersRequest(
    Long gisuId,
    @Positive Integer countPerPartPerSchool,
    List<@NotNull ChallengerPart> parts,
    List<Long> chapterIds,
    @Positive Integer countPerTrackPerSchool,
    List<@NotNull ChallengerTrack> tracks
) {

    public SeedChallengersCommand toCommand() {
        return new SeedChallengersCommand(gisuId, countPerPartPerSchool, parts, chapterIds,
            countPerTrackPerSchool, tracks);
    }
}
