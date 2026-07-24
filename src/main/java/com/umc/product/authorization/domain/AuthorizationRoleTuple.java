package com.umc.product.authorization.domain;

import java.time.Instant;
import java.util.Objects;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

public record AuthorizationRoleTuple(
    ChallengerRoleType roleType,
    OrganizationType organizationType,
    Long organizationId,
    ChallengerPart responsiblePart,
    long gisuId,
    Instant gisuStartAt,
    Instant gisuEndAt
) {
    public AuthorizationRoleTuple {
        Objects.requireNonNull(roleType);
        Objects.requireNonNull(organizationType);
        validateGisu(gisuId, gisuStartAt, gisuEndAt);
    }

    public boolean isActiveAt(Instant evaluatedAt) {
        Objects.requireNonNull(evaluatedAt);
        return !evaluatedAt.isBefore(gisuStartAt) && evaluatedAt.isBefore(gisuEndAt);
    }

    private static void validateGisu(long gisuId, Instant startAt, Instant endAt) {
        if (gisuId <= 0) {
            throw new IllegalArgumentException("gisuId는 양수여야 합니다.");
        }
        Objects.requireNonNull(startAt);
        Objects.requireNonNull(endAt);
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("기수 시작 시각은 종료 시각보다 빨라야 합니다.");
        }
    }
}
