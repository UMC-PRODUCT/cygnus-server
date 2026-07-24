package com.umc.product.community.application.authorization;

import java.time.Instant;
import java.util.Optional;

import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SubjectAttributes;

record CommunityAuthorizationContext(
    CommunityPolicyAction action,
    SubjectAttributes legacySubject,
    Optional<AuthorizationSubjectSnapshot> subject,
    boolean authorHasChallengerHistory,
    Long authorMemberId,
    Instant evaluatedAt
) {
}
