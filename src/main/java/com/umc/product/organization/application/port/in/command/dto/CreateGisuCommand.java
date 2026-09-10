package com.umc.product.organization.application.port.in.command.dto;

import java.time.Instant;

import com.umc.product.common.domain.enums.GisuLearningType;

import lombok.Builder;

@Builder
public record CreateGisuCommand(
    Long generation,
    Instant startAt,
    Instant endAt,
    GisuLearningType learningType
) {
    public CreateGisuCommand {
        learningType = learningType == null ? GisuLearningType.PART : learningType;
    }

    public CreateGisuCommand(Long generation, Instant startAt, Instant endAt) {
        this(generation, startAt, endAt, GisuLearningType.PART);
    }
}
