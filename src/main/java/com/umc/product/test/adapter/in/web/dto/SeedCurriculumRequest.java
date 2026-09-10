package com.umc.product.test.adapter.in.web.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.test.application.port.in.command.dto.SeedCurriculumCommand;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record SeedCurriculumRequest(
    Long gisuId,
    @Positive @Max(16) int weeksPerCurriculum,
    @PositiveOrZero @Max(10) int missionsPerWorkbook,
    List<ChallengerPart> parts,
    List<ChallengerTrack> tracks,
    Long releaseRequesterMemberId
) {

    public SeedCurriculumCommand toCommand() {
        return new SeedCurriculumCommand(
            gisuId,
            weeksPerCurriculum,
            missionsPerWorkbook,
            parts,
            tracks,
            releaseRequesterMemberId
        );
    }
}
