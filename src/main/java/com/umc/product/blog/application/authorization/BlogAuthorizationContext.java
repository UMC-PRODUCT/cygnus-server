package com.umc.product.blog.application.authorization;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;

public record BlogAuthorizationContext(
    BlogPolicyAction action,
    boolean author,
    boolean legacySuperAdmin,
    Optional<AuthorizationSubjectSnapshot> subject,
    Instant evaluatedAt
) {

    public BlogAuthorizationContext {
        Objects.requireNonNull(action);
        subject = Objects.requireNonNull(subject);
        Objects.requireNonNull(evaluatedAt);
        if (subject.isPresent() && !subject.orElseThrow().evaluatedAt().equals(evaluatedAt)) {
            throw new IllegalArgumentException(
                "Blog policy subject와 context의 evaluatedAt이 일치하지 않습니다.");
        }
    }
}
