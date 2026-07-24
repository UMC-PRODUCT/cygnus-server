package com.umc.product.certificate.application.authorization;

import java.util.Objects;

import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;

public record CertificateAuthorizationContext(
    CertificatePolicyAction action,
    long targetGisuId,
    AuthorizationSubjectSnapshot subject
) {

    public CertificateAuthorizationContext {
        Objects.requireNonNull(action);
        if (targetGisuId <= 0) {
            throw new IllegalArgumentException("Certificate targetGisuId는 양수여야 합니다.");
        }
        Objects.requireNonNull(subject);
    }

    public boolean legacyCentralCoreInTargetGisu() {
        return subject.roles().stream()
            .anyMatch(role -> role.gisuId() == targetGisuId
                && role.roleType().isAtLeastCentralCore());
    }

    public boolean activeCentralCoreInTargetGisu() {
        return subject.roles().stream()
            .filter(role -> role.isActiveAt(subject.evaluatedAt()))
            .anyMatch(role -> role.gisuId() == targetGisuId
                && role.roleType().isAtLeastCentralCore());
    }
}
