package com.umc.product.feedback.application.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;

record FeedbackAuthorizationContext(
    FeedbackPolicyAction action,
    long memberId,
    Long targetGisuId,
    Long targetGeneration,
    Optional<UserFeedbackTargetType> resourceTargetType,
    boolean legacyCentralMember,
    List<ChallengerInfo> legacyChallengerHistory,
    AuthorizationSubjectSnapshot subject,
    Instant evaluatedAt
) {

    FeedbackAuthorizationContext {
        resourceTargetType = Optional.ofNullable(resourceTargetType).orElse(Optional.empty());
        legacyChallengerHistory = List.copyOf(legacyChallengerHistory);
    }
}
