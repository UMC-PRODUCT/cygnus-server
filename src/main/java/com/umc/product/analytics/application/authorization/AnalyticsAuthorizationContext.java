package com.umc.product.analytics.application.authorization;

import java.time.Instant;
import java.util.Optional;

import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SubjectAttributes;

record AnalyticsAuthorizationContext(
    AnalyticsPolicyAction action,
    SubjectAttributes legacySubject,
    Optional<AuthorizationSubjectSnapshot> subject,
    Instant evaluatedAt
) {
}
