package com.umc.product.authorization.domain;

import java.time.Instant;
import java.util.Objects;

import com.umc.product.common.domain.enums.ChallengerPart;

public record AuthorizationChallengerTuple(
    long challengerId,
    long gisuId,
    long chapterId,
    ChallengerPart part,
    Instant gisuStartAt,
    Instant gisuEndAt
) {
    public AuthorizationChallengerTuple {
        if (challengerId <= 0 || gisuId <= 0 || chapterId <= 0) {
            throw new IllegalArgumentException("Challenger policy tuple ID는 양수여야 합니다.");
        }
        Objects.requireNonNull(part);
        Objects.requireNonNull(gisuStartAt);
        Objects.requireNonNull(gisuEndAt);
        if (!gisuStartAt.isBefore(gisuEndAt)) {
            throw new IllegalArgumentException("기수 시작 시각은 종료 시각보다 빨라야 합니다.");
        }
    }

    public boolean isActiveAt(Instant evaluatedAt) {
        Objects.requireNonNull(evaluatedAt);
        return !evaluatedAt.isBefore(gisuStartAt) && evaluatedAt.isBefore(gisuEndAt);
    }
}
