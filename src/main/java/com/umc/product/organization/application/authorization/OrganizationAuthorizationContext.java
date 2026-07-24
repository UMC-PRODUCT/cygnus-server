package com.umc.product.organization.application.authorization;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SubjectAttributes;

public record OrganizationAuthorizationContext(
    OrganizationPolicyAction action,
    SubjectAttributes legacySubject,
    Optional<AuthorizationSubjectSnapshot> subject,
    Instant evaluatedAt
) {

    public OrganizationAuthorizationContext {
        Objects.requireNonNull(action);
        Objects.requireNonNull(legacySubject);
        subject = Objects.requireNonNull(subject);
        Objects.requireNonNull(evaluatedAt);
        if (subject.isPresent() && !subject.orElseThrow().evaluatedAt().equals(evaluatedAt)) {
            throw new IllegalArgumentException(
                "Organization policy subject와 context의 evaluatedAt이 일치하지 않습니다.");
        }
    }
}
