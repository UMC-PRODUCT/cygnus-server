package com.umc.product.project.application.authorization;

import java.time.Instant;
import java.util.Objects;

import com.umc.product.common.domain.enums.ChallengerPart;

public record ProjectPolicyChallengerTuple(
    long challengerId,
    long gisuId,
    long chapterId,
    ChallengerPart part,
    Instant gisuStartAt,
    Instant gisuEndAt
) {
    public ProjectPolicyChallengerTuple {
        Objects.requireNonNull(part);
        Objects.requireNonNull(gisuStartAt);
        Objects.requireNonNull(gisuEndAt);
        if (!gisuStartAt.isBefore(gisuEndAt)) {
            throw new IllegalArgumentException("기수 시작 시각은 종료 시각보다 빨라야 합니다.");
        }
    }

    public boolean isActiveAt(Instant evaluatedAt) {
        return !evaluatedAt.isBefore(gisuStartAt) && evaluatedAt.isBefore(gisuEndAt);
    }
}
