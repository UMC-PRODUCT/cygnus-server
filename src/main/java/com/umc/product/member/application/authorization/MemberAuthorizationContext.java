package com.umc.product.member.application.authorization;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SubjectAttributes;

public record MemberAuthorizationContext(
    MemberPolicyAction action,
    SubjectAttributes legacySubject,
    Optional<AuthorizationSubjectSnapshot> subject,
    Instant evaluatedAt
) {

    public MemberAuthorizationContext {
        Objects.requireNonNull(action);
        Objects.requireNonNull(legacySubject);
        subject = Objects.requireNonNull(subject);
        Objects.requireNonNull(evaluatedAt);
    }
}
