package com.umc.product.project.application.authorization;

import java.time.Instant;
import java.util.Objects;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

public record ProjectPolicyRoleTuple(
    ChallengerRoleType roleType,
    OrganizationType organizationType,
    Long organizationId,
    ChallengerPart responsiblePart,
    long gisuId,
    Instant gisuStartAt,
    Instant gisuEndAt
) {
    public ProjectPolicyRoleTuple {
        Objects.requireNonNull(roleType);
        Objects.requireNonNull(organizationType);
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
