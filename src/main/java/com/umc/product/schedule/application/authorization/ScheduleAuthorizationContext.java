package com.umc.product.schedule.application.authorization;

import java.time.Instant;
import java.util.Optional;

import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SubjectAttributes;

record ScheduleAuthorizationContext(
    SchedulePolicyAction action,
    SubjectAttributes legacySubject,
    Optional<AuthorizationSubjectSnapshot> subject,
    boolean resourceSpecified,
    Long targetGisuId,
    boolean author,
    boolean participant,
    Instant evaluatedAt
) {
}
