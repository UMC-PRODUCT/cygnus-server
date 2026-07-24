package com.umc.product.notification.application.authorization;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SubjectAttributes;

public record NotificationAuthorizationContext(
    NotificationPolicyAction action,
    Optional<SubjectAttributes> legacySubject,
    Optional<AuthorizationSubjectSnapshot> subject,
    boolean tokenOwner,
    Instant evaluatedAt
) {

    public NotificationAuthorizationContext {
        Objects.requireNonNull(action);
        legacySubject = Objects.requireNonNull(legacySubject);
        subject = Objects.requireNonNull(subject);
        Objects.requireNonNull(evaluatedAt);
        if (subject.isPresent() && !subject.orElseThrow().evaluatedAt().equals(evaluatedAt)) {
            throw new IllegalArgumentException(
                "Notification policy subject와 context의 evaluatedAt이 일치하지 않습니다.");
        }
        if (action == NotificationPolicyAction.SEND_FCM && legacySubject.isEmpty()) {
            throw new IllegalArgumentException("FCM 발송 legacy subject가 없습니다.");
        }
    }
}
