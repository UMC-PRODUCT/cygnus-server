package com.umc.product.term.application.authorization;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SubjectAttributes;

public record TermAuthorizationContext(
    SubjectAttributes legacySubject,
    Optional<AuthorizationSubjectSnapshot> subject,
    Instant evaluatedAt
) {

    public TermAuthorizationContext {
        Objects.requireNonNull(legacySubject);
        subject = Objects.requireNonNull(subject);
        Objects.requireNonNull(evaluatedAt);
        if (subject.isPresent() && !subject.orElseThrow().evaluatedAt().equals(evaluatedAt)) {
            throw new IllegalArgumentException("Term policy subject와 context의 evaluatedAt이 일치하지 않습니다.");
        }
    }
}
