package com.umc.product.maintenance.application.authorization;

import java.util.Objects;

import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;

public record MaintenanceAuthorizationContext(AuthorizationSubjectSnapshot subject) {

    public MaintenanceAuthorizationContext {
        Objects.requireNonNull(subject);
    }
}
