package com.umc.product.project.application.port.in.command.dto;

import java.time.Instant;
import java.util.Objects;

import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;

import lombok.Builder;

@Builder
public record CreateProjectMatchingRoundCommand(
    Long requesterMemberId,
    String name,
    String description,
    MatchingType type,
    MatchingPhase phase,
    Long gisuId,
    Long chapterId,
    Instant startsAt,
    Instant endsAt,
    Instant decisionDeadline
) {
    public CreateProjectMatchingRoundCommand {
        Objects.requireNonNull(requesterMemberId, "requesterMemberId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(phase, "phase must not be null");
        Objects.requireNonNull(gisuId, "gisuId must not be null");
        Objects.requireNonNull(chapterId, "chapterId must not be null");
        Objects.requireNonNull(startsAt, "startsAt must not be null");
        Objects.requireNonNull(endsAt, "endsAt must not be null");
        Objects.requireNonNull(decisionDeadline, "decisionDeadline must not be null");
    }
}
