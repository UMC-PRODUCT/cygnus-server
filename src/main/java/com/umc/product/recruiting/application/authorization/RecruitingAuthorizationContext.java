package com.umc.product.recruiting.application.authorization;

import java.time.Instant;
import java.util.Optional;

import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SubjectAttributes;

record RecruitingAuthorizationContext(
    RecruitingPolicyAction action,
    long memberId,
    Optional<SubjectAttributes> legacySubject,
    Optional<AuthorizationSubjectSnapshot> subject,
    boolean resourceSpecified,
    Long targetGisuId,
    Long targetSchoolId,
    Instant evaluatedAt
) {
}
