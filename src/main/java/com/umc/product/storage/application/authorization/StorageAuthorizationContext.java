package com.umc.product.storage.application.authorization;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;

public record StorageAuthorizationContext(
    boolean uploader,
    Optional<AuthorizationSubjectSnapshot> subject,
    Instant evaluatedAt
) {

    public StorageAuthorizationContext {
        subject = Objects.requireNonNull(subject);
        Objects.requireNonNull(evaluatedAt);
        if (subject.isPresent() && !subject.orElseThrow().evaluatedAt().equals(evaluatedAt)) {
            throw new IllegalArgumentException(
                "Storage policy subject와 context의 evaluatedAt이 일치하지 않습니다.");
        }
    }

    public boolean superAdmin() {
        return subject.map(AuthorizationSubjectSnapshot::isSuperAdmin).orElse(false);
    }
}
