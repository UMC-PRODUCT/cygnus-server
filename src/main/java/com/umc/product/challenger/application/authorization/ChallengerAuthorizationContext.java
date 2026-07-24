package com.umc.product.challenger.application.authorization;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SubjectAttributes;

public record ChallengerAuthorizationContext(
    ChallengerPolicyAction action,
    SubjectAttributes legacySubject,
    Optional<AuthorizationSubjectSnapshot> subject,
    Long targetGisuId,
    Long targetSchoolId,
    Instant evaluatedAt
) {

    public ChallengerAuthorizationContext {
        Objects.requireNonNull(action);
        Objects.requireNonNull(legacySubject);
        subject = Objects.requireNonNull(subject);
        Objects.requireNonNull(evaluatedAt);
        if (subject.isPresent() && !subject.orElseThrow().evaluatedAt().equals(evaluatedAt)) {
            throw new IllegalArgumentException(
                "Challenger policy subject와 context의 evaluatedAt이 일치하지 않습니다.");
        }
    }
}
