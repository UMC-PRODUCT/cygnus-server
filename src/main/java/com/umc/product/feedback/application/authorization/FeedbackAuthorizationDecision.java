package com.umc.product.feedback.application.authorization;

import java.util.Optional;

import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;

record FeedbackAuthorizationDecision(
    boolean allowed,
    Optional<UserFeedbackTargetType> targetType
) {

    static FeedbackAuthorizationDecision allow(UserFeedbackTargetType targetType) {
        return new FeedbackAuthorizationDecision(true, Optional.of(targetType));
    }

    static FeedbackAuthorizationDecision allowWithoutOutcome() {
        return new FeedbackAuthorizationDecision(true, Optional.empty());
    }

    static FeedbackAuthorizationDecision deny() {
        return new FeedbackAuthorizationDecision(false, Optional.empty());
    }
}
